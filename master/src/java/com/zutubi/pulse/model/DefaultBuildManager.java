package com.zutubi.pulse.model;

import com.zutubi.pulse.MasterBuildPaths;
import com.zutubi.pulse.bootstrap.DatabaseConsole;
import com.zutubi.pulse.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.core.model.*;
import com.zutubi.pulse.events.Event;
import com.zutubi.pulse.events.EventListener;
import com.zutubi.pulse.events.EventManager;
import com.zutubi.pulse.events.build.BuildCompletedEvent;
import com.zutubi.pulse.model.persistence.ArtifactDao;
import com.zutubi.pulse.model.persistence.BuildResultDao;
import com.zutubi.pulse.model.persistence.ChangelistDao;
import com.zutubi.pulse.model.persistence.FileArtifactDao;
import com.zutubi.pulse.scheduling.Scheduler;
import com.zutubi.pulse.scheduling.SchedulingException;
import com.zutubi.pulse.scheduling.SimpleTrigger;
import com.zutubi.pulse.scheduling.Trigger;
import com.zutubi.pulse.scheduling.tasks.CleanupBuilds;
import com.zutubi.pulse.util.Constants;
import com.zutubi.pulse.util.FileSystemUtils;
import com.zutubi.pulse.util.logging.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 *
 */
public class DefaultBuildManager implements BuildManager, EventListener
{
    private static final Logger LOG = Logger.getLogger(DefaultBuildManager.class);

    private BuildResultDao buildResultDao;
    private ArtifactDao artifactDao;
    private FileArtifactDao fileArtifactDao;
    private ChangelistDao changelistDao;
    private ProjectManager projectManager;
    private Scheduler scheduler;
    private MasterConfigurationManager configurationManager;
    private EventManager eventManager;

    private DatabaseConsole databaseConsole;

    private BlockingQueue<CleanupRequest> cleanupQueue = new LinkedBlockingQueue<CleanupRequest>();

    private static final String CLEANUP_NAME = "cleanup";
    private static final String CLEANUP_GROUP = "services";
    private static final long CLEANUP_FREQUENCY = Constants.HOUR;

    private static final Map<Project, Object> runningCleanups = new HashMap<Project, Object>();

    public void init()
    {
        eventManager.register(this);

        // register a schedule for cleaning up old build results.
        // check if the trigger exists. if not, create and schedule.
        Trigger trigger = scheduler.getTrigger(CLEANUP_NAME, CLEANUP_GROUP);
        if (trigger == null)
        {
            // initialise the trigger.
            trigger = new SimpleTrigger(CLEANUP_NAME, CLEANUP_GROUP, CLEANUP_FREQUENCY);
            trigger.setTaskClass(CleanupBuilds.class);

            try
            {
                scheduler.schedule(trigger);
            }
            catch (SchedulingException e)
            {
                LOG.severe(e);
            }
        }

        Thread cleanupThread = new Thread(new Runnable()
        {
            @SuppressWarnings({"InfiniteLoopStatement"})
            public void run()
            {
                while(true)
                {
                    try
                    {
                        CleanupRequest request = cleanupQueue.take();
                        if(request.dir.exists() && !FileSystemUtils.rmdir(request.dir))
                        {
                            LOG.warning("Unable to remove directory '" + request.dir.getAbsolutePath() + "'");
                        }
                    }
                    catch (InterruptedException e)
                    {
                        LOG.warning(e);
                    }
                }
            }
        }, "Build Cleanup Service");

        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    public void setBuildResultDao(BuildResultDao dao)
    {
        buildResultDao = dao;
    }

    public void setArtifactDao(ArtifactDao dao)
    {
        artifactDao = dao;
    }

    public void save(BuildResult buildResult)
    {
        buildResultDao.save(buildResult);
    }

    public void save(RecipeResultNode node)
    {
        buildResultDao.save(node);
    }

    public void save(RecipeResult result)
    {
        buildResultDao.save(result);
    }

    public void save(Changelist changelist)
    {
        changelistDao.save(changelist);
    }

    public BuildResult getBuildResult(long id)
    {
        return buildResultDao.findById(id);
    }

    public RecipeResultNode getRecipeResultNode(long id)
    {
        return buildResultDao.findRecipeResultNode(id);
    }

    public RecipeResultNode getResultNodeByResultId(long id)
    {
        return buildResultDao.findResultNodeByResultId(id);
    }

    public RecipeResult getRecipeResult(long id)
    {
        return buildResultDao.findRecipeResult(id);
    }

    public List<BuildResult> getLatestBuildResultsForProject(Project project, int max)
    {
        return buildResultDao.findLatestByProject(project, max);
    }

    public int getBuildCount(Project project, ResultState[] states, PersistentName spec)
    {
        return buildResultDao.getBuildCount(project, states, spec);
    }

    public int getBuildCount(BuildSpecification spec, long after, long upTo)
    {
        return buildResultDao.getBuildCount(spec.getPname(), after, upTo);
    }

    public void fillHistoryPage(HistoryPage page)
    {
        fillHistoryPage(page, new ResultState[]{ResultState.ERROR, ResultState.FAILURE, ResultState.SUCCESS}, null);
    }

    public List<PersistentName> getBuildSpecifications(Project project)
    {
        return buildResultDao.findAllSpecifications(project);
    }

    public void fillHistoryPage(HistoryPage page, ResultState[] states, PersistentName spec)
    {
        page.setTotalBuilds(buildResultDao.getBuildCount(page.getProject(), states, spec));
        page.setResults(buildResultDao.findLatestByProject(page.getProject(), states, spec, page.getFirst(), page.getMax()));
    }

    public List<BuildResult> getLatestCompletedBuildResults(Project project, PersistentName spec, int max)
    {
        return getLatestCompletedBuildResults(project, spec, 0, max);
    }

    public List<BuildResult> getLatestCompletedBuildResults(Project project, PersistentName spec, int first, int max)
    {
        return buildResultDao.findLatestCompleted(project, spec, first, max);        
    }

    public BuildResult getByProjectAndNumber(final Project project, final long number)
    {
        return buildResultDao.findByProjectAndNumber(project, number);
    }

    public BuildResult getByUserAndNumber(User user, long id)
    {
        return buildResultDao.findByUserAndNumber(user, id);
    }

    public CommandResult getCommandResult(long id)
    {
        return buildResultDao.findCommandResult(id);
    }

    public StoredArtifact getArtifact(long id)
    {
        return artifactDao.findById(id);
    }

    public StoredFileArtifact getFileArtifact(long id)
    {
        return fileArtifactDao.findById(id);
    }

    public List<BuildResult> getPersonalBuilds(User user)
    {
        return buildResultDao.findByUser(user);
    }

    public BuildResult getLatestBuildResult(User user)
    {
        List<BuildResult> results = buildResultDao.getLatestByUser(user, null, 1);
        if(results.size() > 0)
        {
            return results.get(0);
        }
        else
        {
            return null;
        }
    }

    public List<BuildResult> queryBuilds(Project[] projects, ResultState[] states, PersistentName[] specs, long earliestStartTime, long latestStartTime, Boolean hasWorkDir, int first, int max, boolean mostRecentFirst)
    {
        return buildResultDao.queryBuilds(projects, states, specs, earliestStartTime, latestStartTime, hasWorkDir, first, max, mostRecentFirst);
    }

    public List<BuildResult> querySpecificationBuilds(Project project, PersistentName spec, ResultState[] states, long lowestNumber, long highestNumber, int first, int max, boolean mostRecentFirst, boolean initialise)
    {
        return buildResultDao.querySpecificationBuilds(project, spec, states, lowestNumber, highestNumber, first, max, mostRecentFirst, initialise);
    }

    public void cleanupBuilds()
    {
        // Lookup project cleanup info, query for old builds, cleanup where necessary
        List<Project> projects = projectManager.getAllProjects();
        for (Project project : projects)
        {
            cleanupBuilds(project);
        }

        // Now check the database is not too close to full
        if (databaseConsole.isEmbedded())
        {
            if(databaseConsole.getDatabaseUsagePercent() > 95.0)
            {
                LOG.warning("The internal database is close to reaching its size limit.  Consider adding more cleanup rules to remove old build information.");
            }
        }
    }

    public Revision getPreviousRevision(Project project, PersistentName specification)
    {
        Revision previousRevision = null;
        int offset = 0;

        while(true)
        {
            List<BuildResult> previousBuildResults = getLatestCompletedBuildResults(project, specification, offset, 1);

            if (previousBuildResults.size() == 1)
            {
                BuildResult previous = previousBuildResults.get(0);
                if (!previous.isUserRevision())
                {
                    BuildScmDetails previousScmDetails = previous.getScmDetails();
                    if (previousScmDetails != null)
                    {
                        previousRevision = previousScmDetails.getRevision();
                        break;
                    }
                }
            }
            else
            {
                break;
            }

            offset++;
        }

        return previousRevision;
    }

    public List<Changelist> getLatestChangesForUser(User user, int max)
    {
        return changelistDao.findLatestByUser(user, max);
    }

    public List<Changelist> getLatestChangesForProject(Project project, int max)
    {
        return changelistDao.findLatestByProject(project, max);
    }

    public List<Changelist> getLatestChangesForProjects(Project[] projects, int max)
    {
        return changelistDao.findLatestByProjects(projects, max);
    }

    public List<Changelist> getChangesForBuild(BuildResult result)
    {
        return changelistDao.findByResult(result.getId());
    }

    public void deleteAllBuilds(Project project)
    {
        List<BuildResult> results;

        MasterBuildPaths paths = new MasterBuildPaths(configurationManager);
        File projectDir = paths.getProjectDir(project);
        scheduleCleanup(projectDir);

        do
        {
            results = buildResultDao.findOldestByProject(project, null, 100, true);
            for (BuildResult r : results)
            {
                cleanupResult(r, false);
            }
        }
        while (results.size() > 0);
    }

    public void deleteAllBuilds(User user)
    {
        MasterBuildPaths paths = new MasterBuildPaths(configurationManager);
        File userDir = paths.getUserDir(user.getId());
        scheduleCleanup(userDir);

        List<BuildResult> results = buildResultDao.findByUser(user);
        for (BuildResult r : results)
        {
            cleanupResult(r, false);
        }
    }

    public Changelist getChangelistByRevision(String serverUid, Revision revision)
    {
        return changelistDao.findByRevision(serverUid, revision);
    }

    public void delete(BuildResult result)
    {
        cleanupResult(result, true);
    }

    public void abortUnfinishedBuilds(Project project, String message)
    {
        List<BuildResult> incompleteBuilds = queryBuilds(new Project[]{ project}, ResultState.getIncompleteStates(), null, -1, -1, null, -1, -1, true);
        for(BuildResult r: incompleteBuilds)
        {
            abortBuild(r, message);
        }
    }

    public void abortUnfinishedBuilds(User user, String message)
    {
        List<BuildResult> incompleteBuilds = buildResultDao.getLatestByUser(user, ResultState.getIncompleteStates(), -1);
        for(BuildResult r: incompleteBuilds)
        {
            abortBuild(r, message);
        }
    }

    private void abortBuild(BuildResult build, String message)
    {
        if (build != null && !build.completed())
        {
            build.abortUnfinishedRecipes();
            build.error(message);
            build.complete();
            build.calculateFeatureCounts();
            save(build);
        }
    }

    public boolean isSpaceAvailableForBuild()
    {
        if (databaseConsole.isEmbedded())
        {
            return databaseConsole.getDatabaseUsagePercent() < 99.5;
        }
        return true;
    }

    public BuildResult getPreviousBuildResult(BuildResult result)
    {
        return buildResultDao.findPreviousBuildResult(result);
    }

    /**
     * Returns true if a cleanup is being run for the specified project, false otherwise.
     *
     * @param project being queried.
     *
     * @return true iff a cleanup is in progress.
     */
    public boolean isCleanupInProgress(Project project)
    {
        return runningCleanups.containsKey(project);
    }

    public CommandResult getCommandResultByArtifact(long artifactId)
    {
        return buildResultDao.findCommandResultByArtifact(artifactId);
    }

    public CommandResult getCommandResult(long recipeResultId, String commandName)
    {
        return buildResultDao.findRecipeResult(recipeResultId).getCommandResult(commandName);
    }

    public StoredArtifact getArtifact(long buildId, String artifactName)
    {
        BuildResult result = buildResultDao.findById(buildId);
        return result.findArtifact(artifactName);
    }

    public StoredArtifact getCommandResultByArtifact(long commandResultId, String artifactName)
    {
        return buildResultDao.findCommandResult(commandResultId).getArtifact(artifactName);
    }

    public Boolean canDecorateArtifact(long artifactId)
    {
        StoredFileArtifact artifact = getFileArtifact(artifactId);
        return artifact != null && artifact.canDecorate();
    }

    // debugging hack: need to work out a better way
    public void executeInTransaction(Runnable runnable)
    {
        runnable.run();
    }

    public BuildResult getLatestBuildResult(BuildSpecification spec)
    {
        return buildResultDao.findLatestByBuildSpec(spec);
    }

    public BuildResult getLatestBuildResult(Project project)
    {
        List<BuildResult> results = getLatestBuildResultsForProject(project, 1);
        if (results.size() > 0)
        {
            return results.get(0);
        }
        return null;
    }

    public BuildResult getLatestBuildResult()
    {
        return buildResultDao.findLatest();
    }

    public BuildResult getLatestSuccessfulBuildResult(BuildSpecification specification)
    {
        return buildResultDao.findLatestSuccessfulBySpecification(specification);
    }

    public BuildResult getLatestSuccessfulBuildResult(Project project)
    {
        return buildResultDao.findLatestSuccessfulByProject(project);
    }

    public BuildResult getLatestSuccessfulBuildResult()
    {
        return buildResultDao.findLatestSuccessful();
    }

    /**
     * Execute the configured cleanup rules for the specified project.
     *
     * @param project   the project to be cleaned up.
     */
    public void cleanupBuilds(Project project)
    {
        try
        {
            runningCleanups.put(project, null);

            List<CleanupRule> rules = project.getCleanupRules();

            for (CleanupRule rule : rules)
            {
                cleanupBuilds(rule, project);
            }
        }
        finally
        {
            runningCleanups.remove(project);
        }
    }

    public void cleanupBuilds(CleanupRule rule)
    {
        // locate the project associated with the cleanup rule.
        Project project = projectManager.getProjectByCleanupRule(rule);
        try
        {
            runningCleanups.put(project, null);
            cleanupBuilds(rule, project);
        }
        finally
        {
            runningCleanups.remove(project);
        }
    }

    public void cleanupBuilds(User user)
    {
        int count = buildResultDao.getCompletedResultCount(user);
        int max = user.getMyBuildsCount();
        if(count > max)
        {
            List<BuildResult> results = buildResultDao.getOldestCompletedBuilds(user, count - max);
            for(BuildResult result: results)
            {
                cleanupResult(result, true);
            }
        }
    }

    private synchronized void cleanupBuilds(CleanupRule rule, Project project)
    {
        List<BuildResult> oldBuilds = rule.getMatchingResults(project, buildResultDao);

        for (BuildResult build : oldBuilds)
        {
            if (rule.getWorkDirOnly())
            {
                cleanupWork(build);
            }
            else
            {
                cleanupResult(build, true);
            }
        }
    }

    public boolean canCancel(BuildResult build, User user)
    {
        if(build.isPersonal())
        {
            return build.getUser().equals(user);
        }
        else
        {
            try
            {
                projectManager.checkWrite(build.getProject());
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    private void cleanupResult(BuildResult build, boolean rmdir)
    {
        MasterBuildPaths paths = new MasterBuildPaths(configurationManager);
        File buildDir = paths.getBuildDir(build);
        if (rmdir && buildDir.exists())
        {
            scheduleCleanup(buildDir);
        }

        if(build.isPersonal())
        {
            File patch = paths.getUserPatchFile(build.getUser().getId(), build.getNumber());
            patch.delete();
        }
        else
        {
            // Remove records of this build from changelists
            BuildScmDetails scmDetails = build.getScmDetails();
            if(scmDetails != null)
            {
                List<Changelist> changelists = changelistDao.findByResult(build.getId());
                for(Changelist change: changelists)
                {
                    change.removeResultId(build.getId());
                    changelistDao.save(change);
                }
            }
        }

        buildResultDao.delete(build);
    }

    private void cleanupWork(BuildResult build)
    {
        build.setHasWorkDir(false);
        buildResultDao.save(build);
        MasterBuildPaths paths = new MasterBuildPaths(configurationManager);
        cleanupWorkForNodes(paths, build, build.getRoot().getChildren());
    }

    private void cleanupWorkForNodes(MasterBuildPaths paths, BuildResult build, List<RecipeResultNode> nodes)
    {
        for (RecipeResultNode node : nodes)
        {
            File workDir = paths.getBaseDir(build, node.getResult().getId());
            if (workDir.exists())
            {
                scheduleCleanup(workDir);
            }
            cleanupWorkForNodes(paths, build, node.getChildren());
        }
    }

    private void scheduleCleanup(File dir)
    {
        File dead = new File(dir + ".dead");
        dir.renameTo(dead);
        try
        {
            cleanupQueue.put(new CleanupRequest(dead));
        }
        catch (InterruptedException e)
        {
            LOG.warning(e);
        }
    }

    public void handleEvent(Event evt)
    {
        BuildCompletedEvent completedEvent = (BuildCompletedEvent) evt;
        BuildResult result = completedEvent.getResult();
        if(result.isPersonal())
        {
            cleanupBuilds(result.getUser());
        }
        else
        {
            cleanupBuilds(result.getProject());
        }
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{BuildCompletedEvent.class};
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public void setProjectManager(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    public void setChangelistDao(ChangelistDao changelistDao)
    {
        this.changelistDao = changelistDao;
    }

    public void setFileArtifactDao(FileArtifactDao fileArtifactDao)
    {
        this.fileArtifactDao = fileArtifactDao;
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void setEventManager(EventManager eventManager)
    {
        this.eventManager = eventManager;
    }

    public void setDatabaseConsole(DatabaseConsole databaseConsole)
    {
        this.databaseConsole = databaseConsole;
    }

    private class CleanupRequest
    {
        private File dir;

        public CleanupRequest(File dir)
        {
            this.dir = dir;
        }
    }
}
