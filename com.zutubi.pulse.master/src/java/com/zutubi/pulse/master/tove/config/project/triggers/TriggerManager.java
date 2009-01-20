package com.zutubi.pulse.master.tove.config.project.triggers;

import com.zutubi.events.Event;
import com.zutubi.events.EventListener;
import com.zutubi.events.EventManager;
import com.zutubi.pulse.core.api.PulseRuntimeException;
import com.zutubi.pulse.core.spring.SpringComponentContext;
import com.zutubi.tove.events.ConfigurationEventSystemStartedEvent;
import com.zutubi.pulse.master.scheduling.Scheduler;
import com.zutubi.pulse.master.scheduling.SchedulingException;
import com.zutubi.pulse.master.scheduling.Trigger;
import com.zutubi.tove.config.*;
import com.zutubi.util.logging.Logger;

/**
 * 
 *
 */
public class TriggerManager implements ExternalStateManager<TriggerConfiguration>, EventListener
{
    private static final Logger LOG = Logger.getLogger(TriggerManager.class);

    private Scheduler scheduler;

    public void registerConfigListeners(ConfigurationProvider configurationProvider)
    {
        TypeListener<TriggerConfiguration> listener = new TypeAdapter<TriggerConfiguration>(TriggerConfiguration.class)
        {
            public void postDelete(TriggerConfiguration instance)
            {
                TriggerManager.this.delete(instance.getTriggerId());
            }

            public void postSave(TriggerConfiguration instance, boolean nested)
            {
                try
                {
                    Trigger trigger = scheduler.getTrigger(instance.getTriggerId());
                    instance.update(trigger);

                    // on an update, the details of the trigger will have changed.  We need to update the
                    // scheduler accordingly.
                    scheduler.update(trigger);
                }
                catch (SchedulingException e)
                {
                    LOG.severe(e);
                }
            }
        };
        listener.register(configurationProvider, true);
    }

    private void delete(long id)
    {
        try
        {
            Trigger trigger = scheduler.getTrigger(id);
            scheduler.unschedule(trigger);
        }
        catch (SchedulingException e)
        {
            LOG.severe(e);
        }
    }

    public long createState(TriggerConfiguration instance)
    {
        try
        {
            SpringComponentContext.autowire(instance);
            Trigger trigger = instance.newTrigger();
            scheduler.schedule(trigger);
            return trigger.getId();
        }
        catch (SchedulingException e)
        {
            LOG.severe(e);
            throw new PulseRuntimeException(e);
        }
    }

    public void rollbackState(long id)
    {
        delete(id);
    }

    public Object getState(long id)
    {
        return scheduler.getTrigger(id);
    }

    public void handleEvent(Event event)
    {
        registerConfigListeners(((ConfigurationEventSystemStartedEvent)event).getConfigurationProvider());
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{ ConfigurationEventSystemStartedEvent.class };
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public void setEventManager(EventManager eventManager)
    {
        eventManager.register(this);
    }

    public void setConfigurationStateManager(ConfigurationStateManager configurationStateManager)
    {
        configurationStateManager.register(TriggerConfiguration.class, this);
    }
}