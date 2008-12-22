package com.zutubi.pulse;

import com.zutubi.pulse.agent.*;
import com.zutubi.pulse.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.core.BuildException;
import com.zutubi.pulse.core.BuildRevision;
import com.zutubi.pulse.core.RecipeRequest;
import com.zutubi.pulse.core.Stoppable;
import com.zutubi.pulse.core.model.Revision;
import com.zutubi.pulse.events.*;
import com.zutubi.pulse.events.EventListener;
import com.zutubi.pulse.events.build.RecipeDispatchedEvent;
import com.zutubi.pulse.events.build.RecipeErrorEvent;
import com.zutubi.pulse.events.build.RecipeStatusEvent;
import com.zutubi.pulse.model.*;
import com.zutubi.pulse.scm.SCMChangeEvent;
import com.zutubi.pulse.scm.SCMException;
import com.zutubi.pulse.scm.SCMServer;
import com.zutubi.pulse.scm.SCMServerUtils;
import com.zutubi.pulse.util.Constants;
import com.zutubi.pulse.util.logging.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A recipe queue that runs an independent thread to manage the dispatching
 * of recipes.
 */
public class ThreadedRecipeQueue implements Runnable, RecipeQueue, EventListener, Stoppable
{
    private static final Logger LOG = Logger.getLogger(ThreadedRecipeQueue.class);

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition lockCondition = lock.newCondition();

    /**
     * The internal queue of dispatch requests.
     */
    private final DispatchQueue dispatchQueue = new DispatchQueue();

    private ExecutorService executor;

    private boolean stopRequested = false;
    private boolean isRunning = false;
    /**
     * * Maximum number of seconds between checks of the queue.  Usually
     * checks occur due to the condition being flagged, but we need to
     * wake up periodically to enforce timeouts.
     */
    private int sleepInterval = 60;
    /**
     * Maximum number of millis to leave a request that cannot be satisfied
     * in the queue.  This is based on how long there has been no capable
     * agent available (not on how long the request has been queued).  If the
     * timeout is 0, unsatisfiable requests will be rejected immediately.  If
     * the timeout is negative, requests will never time out.
     */
    private long unsatisfiableTimeout = 0;

    private BlockingQueue<DispatchedRequest> dispatchedQueue = new LinkedBlockingQueue<DispatchedRequest>();

    private AgentManager agentManager;
    private EventManager eventManager;
    private MasterLocationProvider masterLocationProvider;
    private AgentSorter agentSorter = new DefaultAgentSorter();

    public ThreadedRecipeQueue()
    {

    }

    public void init()
    {
        try
        {
            // Get all agents
            for (Agent a : agentManager.getOnlineAgents())
            {
                updateTimeoutsForAgent(a);
            }

            eventManager.register(this);

            Thread dispatcherThread = new Thread(new Dispatcher(), "Recipe Dispatcher Service");
            dispatcherThread.setDaemon(true);
            dispatcherThread.start();

            start();
        }
        catch (Exception e)
        {
            LOG.error(e);
        }
    }

    public void start()
    {
        if (isRunning())
        {
            throw new IllegalStateException("The queue is already running.");
        }
        LOG.debug("start();");
        executor = Executors.newSingleThreadExecutor();
        executor.execute(this);
    }

    public void stop()
    {
        stop(true);
    }

    /**
     * Enqueue a new recipe dispatch request.
     *
     * @param dispatchRequest the request to be enqueued
     */
    public void enqueue(RecipeDispatchRequest dispatchRequest)
    {
        RecipeErrorEvent error = null;

        try
        {
            determineRevision(dispatchRequest);

            lock.lock();
            try
            {
                if (requestMayBeFulfilled(dispatchRequest))
                {
                    addToQueue(dispatchRequest);
                }
                else
                {
                    if (unsatisfiableTimeout == 0)
                    {
                        error = new RecipeErrorEvent(this, dispatchRequest.getRequest().getId(), "No online agent is capable of executing the build stage");
                    }
                    else
                    {
                        if (unsatisfiableTimeout > 0)
                        {
                            dispatchRequest.setTimeout(System.currentTimeMillis() + unsatisfiableTimeout);
                        }

                        addToQueue(dispatchRequest);
                    }
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        catch (Exception e)
        {
            error = new RecipeErrorEvent(this, dispatchRequest.getRequest().getId(), "Unable to determine revision to build: " + e.getMessage());
        }


        if (error != null)
        {
            // Publish outside the lock.
            eventManager.publish(error);
        }
    }

    private void addToQueue(RecipeDispatchRequest dispatchRequest)
    {
        dispatchQueue.add(dispatchRequest);
        dispatchRequest.queued();
        lockCondition.signal();
    }

    private void determineRevision(RecipeDispatchRequest dispatchRequest) throws BuildException, SCMException
    {
        BuildRevision buildRevision = dispatchRequest.getRevision();
        if (!buildRevision.isInitialised())
        {
            // Let's initialise it
            eventManager.publish(new RecipeStatusEvent(this, dispatchRequest.getRequest().getId(), "Initialising build revision..."));
            Project project = dispatchRequest.getBuild().getProject();
            Scm scm = project.getScm();
            SCMServer server = null;
            try
            {
                server = scm.createServer();
                Revision revision = server.getLatestRevision();
                // May throw a BuildException
                updateRevision(dispatchRequest, revision);
                eventManager.publish(new RecipeStatusEvent(this, dispatchRequest.getRequest().getId(), "Revision initialised to '" + revision.getRevisionString() + "'"));
            }
            finally
            {
                SCMServerUtils.close(server);
            }
        }
    }

    private void updateRevision(RecipeDispatchRequest dispatchRequest, Revision revision) throws BuildException
    {
        Project project = dispatchRequest.getBuild().getProject();
        String pulseFile = project.getPulseFileDetails().getPulseFile(dispatchRequest.getRequest().getId(), project, revision, null);
        dispatchRequest.getRevision().update(revision, pulseFile);
    }

    public List<RecipeDispatchRequest> takeSnapshot()
    {
        return dispatchQueue.snapshot();
    }

    public boolean cancelRequest(long id)
    {
        boolean removed = false;

        try
        {
            lock.lock();
            RecipeDispatchRequest removeRequest = null;

            for (RecipeDispatchRequest request : dispatchQueue)
            {
                if (request.getRequest().getId() == id)
                {
                    removeRequest = request;
                    break;
                }
            }

            if (removeRequest != null)
            {
                dispatchQueue.remove(removeRequest);
                removed = true;
            }
        }
        finally
        {
            lock.unlock();
        }

        return removed;
    }

    void updateTimeoutsForAgent(Agent agent)
    {
        lock.lock();
        try
        {
            resetTimeouts(agent);
            lockCondition.signal();
        }
        finally
        {
            lock.unlock();
        }
    }

    private void resetTimeouts(Agent agent)
    {
        for (RecipeDispatchRequest request : dispatchQueue)
        {
            if (request.hasTimeout() && request.getHostRequirements().fulfilledBy(request, agent.getBuildService()))
            {
                request.clearTimeout();
            }
        }
    }

    void offline(Agent agent)
    {
        List<RecipeDispatchRequest> removedRequests = null;

        lock.lock();
        try
        {
            if (unsatisfiableTimeout == 0)
            {
                removedRequests = removeUnfulfillable();
            }
            else if (unsatisfiableTimeout > 0)
            {
                checkQueuedTimeouts(System.currentTimeMillis() + unsatisfiableTimeout);
            }

            lockCondition.signal();
        }
        finally
        {
            lock.unlock();
        }

        if (removedRequests != null)
        {
            publishUnfulfillable(removedRequests);
        }
    }

    private void checkQueuedTimeouts(long timeout)
    {
        assert (lock.isHeldByCurrentThread());

        for (RecipeDispatchRequest request : dispatchQueue)
        {
            if (!request.hasTimeout() && !requestMayBeFulfilled(request))
            {
                request.setTimeout(timeout);
            }
        }
    }

    private List<RecipeDispatchRequest> removeUnfulfillable()
    {
        assert (lock.isHeldByCurrentThread());

        List<RecipeDispatchRequest> unfulfillable = new LinkedList<RecipeDispatchRequest>();
        for (RecipeDispatchRequest request : dispatchQueue)
        {
            if (!requestMayBeFulfilled(request))
            {
                unfulfillable.add(request);
            }
        }

        dispatchQueue.removeAll(unfulfillable);
        return unfulfillable;
    }

    private boolean requestMayBeFulfilled(RecipeDispatchRequest request)
    {
        eventManager.publish(new RecipeStatusEvent(this, request.getRequest().getId(), "Checking recipe agent requirements..."));
        for (Agent a : agentManager.getOnlineAgents())
        {
            if (request.getHostRequirements().fulfilledBy(request, a.getBuildService()))
            {
                eventManager.publish(new RecipeStatusEvent(this, request.getRequest().getId(), "Requirements satisfied by at least one online agent."));
                return true;
            }
        }

        eventManager.publish(new RecipeStatusEvent(this, request.getRequest().getId(), "No online agents satisfy requirements."));
        return false;
    }

    public void run()
    {
        isRunning = true;
        stopRequested = false;
        LOG.debug("started.");

        // wait for changes to either of the inbound queues. When change detected,
        // copy the new data into the internal queue (to minimize locked time) and
        // start processing.  JS: extended lock time to simplify snapshotting:
        // review iff this leads to a performance issue (seems unlikely).

        while (!stopRequested)
        {
            lock.lock();
            LOG.debug("lock.lock();");
            try
            {
                if (stopRequested)
                {
                    break;
                }

                List<RecipeDispatchRequest> doneRequests = new LinkedList<RecipeDispatchRequest>();
                long currentTime = System.currentTimeMillis();

                for (RecipeDispatchRequest request : dispatchQueue)
                {
                    if (request.hasTimedOut(currentTime))
                    {
                        doneRequests.add(request);
                        eventManager.publish(new RecipeErrorEvent(this, request.getRequest().getId(), "Recipe request timed out waiting for a capable agent to become available"));
                    }
                    else
                    {
                        Iterable<Agent> agentList = agentSorter.sort(agentManager.getAvailableAgents(), request);
                        for (Agent agent : agentList)
                        {
                            BuildService service = agent.getBuildService();

                            // can the request be sent to this service?
                            if (request.getHostRequirements().fulfilledBy(request, service))
                            {
                                if (dispatchRequest(request, agent, doneRequests))
                                {
                                    break;
                                }
                            }
                        }
                    }
                }

                dispatchQueue.removeAll(doneRequests);
                try
                {
                    // Wake up when there is something to do, and also
                    // periodically to check for timed-out requests.
                    LOG.debug("lockCondition.await();");
                    lockCondition.await(sleepInterval, TimeUnit.SECONDS);
                    LOG.debug("lockCondition.unawait();");
                }
                catch (InterruptedException e)
                {
                    LOG.debug("lockCondition.wait() was interrupted: " + e.getMessage());
                }
            }
            finally
            {
                lock.unlock();
                LOG.debug("lock.unlock();");
            }
        }

        executor.shutdown();
        LOG.debug("stopped.");
        isRunning = false;
    }

    private boolean dispatchRequest(RecipeDispatchRequest request, Agent agent, List<RecipeDispatchRequest> dispatchedRequests)
    {
        BuildRevision buildRevision = request.getRevision();
        RecipeRequest recipeRequest = request.getRequest();

        // This must be called before publishing the event.
        // We can no longer update the revision once we have dispatched a
        // request: it is fixed here if not already.
        buildRevision.apply(recipeRequest);
        recipeRequest.prepare(agent.getName());

        // This code cannot handle an agent rejecting the build
        // (the handling was backed outdue to CIB-553 and the fact that
        // agents do not currently reject builds)
        eventManager.publish(new RecipeDispatchedEvent(this, recipeRequest, agent));
        dispatchedRequests.add(request);

        BuildContext context = createBuildContext(request, recipeRequest, buildRevision, agent);
        dispatchedQueue.offer(new DispatchedRequest(recipeRequest, context, agent));

        return true;
    }

    private BuildContext createBuildContext(RecipeDispatchRequest request, RecipeRequest recipeRequest, BuildRevision buildRevision, Agent agent)
    {
        BuildContext context = new BuildContext();
        context.setBuildNumber(request.getBuild().getNumber());
        context.setProjectName(recipeRequest.getProject());
        context.setCleanBuild(isMarkedForCleanBuild(request.getBuildSpecification(), agent));

        BuildReason buildReason = request.getBuild().getReason();
        context.addProperty("build.reason", buildReason.getSummary());
        if (buildReason instanceof TriggerBuildReason)
        {
            context.addProperty("build.trigger", ((TriggerBuildReason) buildReason).getTriggerName());
        }

        context.addProperty("build.revision", buildRevision.getRevision().getRevisionString());
        context.addProperty("build.timestamp", BuildContext.PULSE_BUILD_TIMESTAMP_FORMAT.format(new Date(buildRevision.getTimestamp())));
        context.addProperty("build.timestamp.millis", Long.toString(buildRevision.getTimestamp()));
        context.addProperty("master.url", masterLocationProvider.getMasterUrl());
        context.addProperty("specification.build.count", Integer.toString(request.getBuildSpecification().getBuildCount()));
        context.addProperty("specification.success.count", Integer.toString(request.getBuildSpecification().getSuccessCount()));
        return context;
    }

    private boolean isMarkedForCleanBuild(BuildSpecification buildSpecification, Agent agent)
    {
        if (agent.isSlave())
        {
            return buildSpecification.isForceCleanForSlave(agent.getId());
        }
        else
        {
            return buildSpecification.isForceCleanMaster();
        }
    }

    public void stop(boolean force)
    {
        if (isStopped())
        {
            throw new IllegalStateException("The queue is already stopped.");
        }

        lock.lock();
        try
        {
            LOG.debug("stop();");
            stopRequested = true;
            lockCondition.signal();
        }
        finally
        {
            lock.unlock();
        }
    }

    public boolean isStopped()
    {
        return !isRunning();
    }

    public boolean isRunning()
    {
        return isRunning;
    }

    public int length()
    {
        lock.lock();
        try
        {
            return dispatchQueue.size();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void handleEvent(Event evt)
    {
        if (evt instanceof AgentAvailableEvent)
        {
            handleAvailableEvent();
        }
        else if (evt instanceof AgentConnectivityEvent)
        {
            handleConnectivityEvent((AgentConnectivityEvent) evt);
        }
        else if (evt instanceof AgentResourcesDiscoveredEvent)
        {
            updateTimeoutsForAgent(((AgentResourcesDiscoveredEvent) evt).getAgent());
        }
        else if (evt instanceof SCMChangeEvent)
        {
            handleScmChange((SCMChangeEvent) evt);
        }
    }

    private void handleAvailableEvent()
    {
        lock.lock();
        try
        {
            lockCondition.signal();
        }
        finally
        {
            lock.unlock();
        }
    }

    private void handleConnectivityEvent(AgentConnectivityEvent event)
    {
        if (event instanceof AgentOnlineEvent)
        {
            updateTimeoutsForAgent(event.getAgent());
        }
        else if (event instanceof AgentOfflineEvent)
        {
            offline(event.getAgent());
        }
    }

    private void handleScmChange(SCMChangeEvent event)
    {
        List<RecipeDispatchRequest> rejects = null;
        Scm changedScm = event.getScm();
        lock.lock();
        try
        {
            List<RecipeDispatchRequest> unfulfillable = checkQueueForChanges(changedScm, event, dispatchQueue);
            if (unsatisfiableTimeout == 0)
            {
                dispatchQueue.removeAll(unfulfillable);
                rejects = unfulfillable;
            }
            else if (unsatisfiableTimeout > 0)
            {
                updateTimeouts(unfulfillable, System.currentTimeMillis() + unsatisfiableTimeout);
            }
        }
        finally
        {
            lock.unlock();
        }

        // Publish events outside the lock
        if (rejects != null)
        {
            publishUnfulfillable(rejects);
        }
    }

    private void updateTimeouts(List<RecipeDispatchRequest> requests, long timeout)
    {
        for (RecipeDispatchRequest request : requests)
        {
            if (!request.hasTimeout())
            {
                request.setTimeout(timeout);
            }
        }
    }

    private void publishUnfulfillable(List<RecipeDispatchRequest> unfulfillable)
    {
        for (RecipeDispatchRequest request : unfulfillable)
        {
            eventManager.publish(new RecipeErrorEvent(this, request.getRequest().getId(), "No online agent is capable of executing the build stage"));
        }
    }

    private List<RecipeDispatchRequest> checkQueueForChanges(Scm changedScm, SCMChangeEvent event, DispatchQueue queue)
    {
        List<RecipeDispatchRequest> unfulfillable = new LinkedList<RecipeDispatchRequest>();

        for (RecipeDispatchRequest request : queue)
        {
            Scm requestScm = request.getBuild().getProject().getScm();
            if (!request.getRevision().isFixed() && requestScm.getId() == changedScm.getId())
            {
                try
                {
                    eventManager.publish(new RecipeStatusEvent(this, request.getRequest().getId(), "Change detected while queued, updating build revision to '" + event.getNewRevision() + "'"));
                    updateRevision(request, event.getNewRevision());
                    if (!requestMayBeFulfilled(request))
                    {
                        unfulfillable.add(request);
                    }
                }
                catch (Exception e)
                {
                    // We already have a revision, so this is not fatal.
                    LOG.warning("Unable to check build revision: " + e.getMessage(), e);
                }
            }
        }

        return unfulfillable;
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{
                AgentAvailableEvent.class,
                AgentConnectivityEvent.class,
                AgentResourcesDiscoveredEvent.class,
                SCMChangeEvent.class
        };
    }

    public void setSleepInterval(int sleepInterval)
    {
        this.sleepInterval = sleepInterval;
    }

    public void setUnsatisfiableTimeout(long unsatisfiableTimeout)
    {
        this.unsatisfiableTimeout = unsatisfiableTimeout;
    }

    public void setEventManager(EventManager eventManager)
    {
        this.eventManager = eventManager;
    }

    public void setAgentManager(AgentManager agentManager)
    {
        this.agentManager = agentManager;
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        // Cache the timeout value now: it is refreshed when changed by a
        // call to setUnsatisfiableTimeout.
        long timeout = configurationManager.getAppConfig().getUnsatisfiableRecipeTimeout();
        if (timeout > 0)
        {
            timeout *= Constants.MINUTE;
        }

        this.unsatisfiableTimeout = timeout;
    }

    public void setMasterLocationProvider(MasterLocationProvider masterLocationProvider)
    {
        this.masterLocationProvider = masterLocationProvider;
    }

    public void setAgentSorter(AgentSorter agentSorter)
    {
        this.agentSorter = agentSorter;
    }

    private static class DispatchedRequest
    {
        RecipeRequest recipeRequest;
        BuildContext context;
        Agent agent;

        public DispatchedRequest(RecipeRequest recipeRequest, BuildContext context, Agent agent)
        {
            this.recipeRequest = recipeRequest;
            this.context = context;
            this.agent = agent;
        }
    }

    private class Dispatcher implements Runnable
    {
        public void run()
        {
            while (!stopRequested)
            {
                DispatchedRequest dispatchedRequest;
                try
                {
                    dispatchedRequest = dispatchedQueue.take();
                }
                catch (InterruptedException e)
                {
                    continue;
                }

                try
                {
                    dispatchedRequest.agent.getBuildService().build(dispatchedRequest.recipeRequest, dispatchedRequest.context);
                }
                catch (Exception e)
                {
                    LOG.warning("Unable to dispatch recipe: " + e.getMessage(), e);
                    eventManager.publish(new RecipeErrorEvent(this, dispatchedRequest.recipeRequest.getId(), "Unable to dispatch recipe: " + e.getMessage()));
                }
            }
        }
    }

    /**
     * Allow easy / safe access to a snapshot of the list of recipe dispatch requests.  Changes
     * to the list itself are synchronized so that the snapshot is not taken in the middle of a
     * change.  However, and importantly, the snapshot is not bound to the synchronization
     * taking place within the ThreadedRecipeQueue.
     *
     * See CIB-1401. 
     */
    private class DispatchQueue implements Iterable<RecipeDispatchRequest>
    {
        private final List<RecipeDispatchRequest> list = new LinkedList<RecipeDispatchRequest>();

        public synchronized void add(RecipeDispatchRequest item)
        {
            list.add(item);
        }

        public synchronized void remove(RecipeDispatchRequest item)
        {
            list.remove(item);
        }

        public synchronized void addAll(Collection<RecipeDispatchRequest> items)
        {
            list.addAll(items);
        }

        public synchronized void removeAll(Collection<RecipeDispatchRequest> items)
        {
            list.removeAll(items);
        }

        public synchronized List<RecipeDispatchRequest> snapshot()
        {
            return new LinkedList<RecipeDispatchRequest>(list);
        }

        public Iterator<RecipeDispatchRequest> iterator()
        {
            return list.iterator();
        }

        public synchronized int size()
        {
            return list.size();
        }
    }
}