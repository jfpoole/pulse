package com.cinnamonbob.scheduling;

import com.cinnamonbob.scheduling.persistence.TriggerDao;
import com.cinnamonbob.util.logging.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * <class-comment/>
 */
public class DefaultScheduler
{
    private static final Logger LOG = Logger.getLogger(DefaultScheduler.class.getName());

    private List<SchedulerStrategy> strategies = new LinkedList<SchedulerStrategy>();

    private TriggerHandler triggerHandler;

    private TriggerDao triggerDao;

    /**
     * Register a scheduling strategy that will be used to handle a specific trigger type.
     *
     * @param strategy
     */
    public void register(SchedulerStrategy strategy)
    {
        strategy.setTriggerHandler(triggerHandler);
        strategies.add(strategy);
    }

    public void setStrategies(List<SchedulerStrategy> schedulerStrategies)
    {
        strategies.clear();
        for (SchedulerStrategy strategy : schedulerStrategies)
        {
            strategy.setTriggerHandler(triggerHandler);
            strategies.add(strategy);
        }
    }

    protected void init()
    {
        // ensure that the strategies are correctly configured.
        for (SchedulerStrategy strategy : strategies)
        {
            strategy.setTriggerHandler(triggerHandler);
        }

        for (Trigger trigger : triggerDao.findAll())
        {
            SchedulerStrategy strategy = getStrategy(trigger);
            try
            {
                strategy.schedule(trigger);
                // TODO: there is a very brief opportunity here for a paused trigger to execute.... should close this gap..
                if (trigger.isPaused())
                {
                    strategy.pause(trigger);
                }
            }
            catch (SchedulingException e)
            {
                // not the fact that this trigger is invalid but do not prevent the rest of the triggers from
                // being initialisd.
                LOG.severe("failed to initialise a trigger.", e);
            }
        }
    }

    public Trigger getTrigger(String name, String group)
    {
        return triggerDao.findByNameAndGroup(name, group);
    }

    public Trigger getTrigger(long id)
    {
        return triggerDao.findById(id);
    }

    public List<Trigger> getTriggers(long id)
    {
        return triggerDao.findByProject(id);
    }

    public Trigger getTrigger(long id, String name)
    {
        return triggerDao.findByProjectAndName(id, name);
    }

    public void schedule(Trigger trigger) throws SchedulingException
    {
        if (triggerDao.findByNameAndGroup(trigger.getName(), trigger.getGroup()) != null)
        {
            throw new SchedulingException("A trigger with name " + trigger.getName() + " and group " + trigger.getGroup() + " has already been registered.");
        }

        SchedulerStrategy impl = getStrategy(trigger);
        impl.schedule(trigger);

        // assosiate trigger and task so that task can be retrieved when trigger fires.
        triggerDao.save(trigger);
    }

    public void trigger(Trigger trigger) throws SchedulingException
    {
        triggerHandler.trigger(trigger);
    }

    /**
     * Unschedule a trigger. Only scheduled triggers can be unscheduled.
     *
     * @param trigger
     * @throws SchedulingException
     */
    public void unschedule(Trigger trigger) throws SchedulingException
    {
        assertScheduled(trigger);
        SchedulerStrategy impl = getStrategy(trigger);
        impl.unschedule(trigger);
        triggerDao.delete(trigger);
    }

    public void pause(String group) throws SchedulingException
    {
        for (Trigger trigger : triggerDao.findByGroup(group))
        {
            pause(trigger);
        }
    }

    public void pause(Trigger trigger) throws SchedulingException
    {
        assertScheduled(trigger);
        if (!trigger.isActive())
        {
            return;
        }
        SchedulerStrategy impl = getStrategy(trigger);
        impl.pause(trigger);
    }

    public void resume(String group) throws SchedulingException
    {
        for (Trigger trigger : triggerDao.findByGroup(group))
        {
            resume(trigger);
        }
    }

    public void resume(Trigger trigger) throws SchedulingException
    {
        assertScheduled(trigger);
        if (!trigger.isPaused())
        {
            return;
        }
        SchedulerStrategy impl = getStrategy(trigger);
        impl.resume(trigger);
    }

    /**
     * Retrieve the first registered strategy that is able to handle this trigger.
     *
     * @param trigger
     * @return a scheduler strategy.
     */
    private SchedulerStrategy getStrategy(Trigger trigger)
    {
        for (SchedulerStrategy strategy : strategies)
        {
            if (strategy.canHandle(trigger))
            {
                return strategy;
            }
        }
        return null;
    }

    private void assertScheduled(Trigger trigger) throws SchedulingException
    {
        if (!trigger.isScheduled())
        {
            throw new SchedulingException("The trigger must be scheduled first.");
        }
    }

    /**
     * Set a reference to the required TriggerDao resource.
     *
     * @param triggerDao
     */
    public void setTriggerDao(TriggerDao triggerDao)
    {
        this.triggerDao = triggerDao;
    }

    public void setTriggerHandler(TriggerHandler triggerHandler)
    {
        this.triggerHandler = triggerHandler;
    }
}
