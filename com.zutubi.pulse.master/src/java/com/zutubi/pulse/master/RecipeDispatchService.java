package com.zutubi.pulse.master;

import com.zutubi.events.EventManager;
import com.zutubi.pulse.core.events.RecipeErrorEvent;
import com.zutubi.pulse.master.events.build.RecipeAssignedEvent;
import com.zutubi.pulse.master.events.build.RecipeDispatchedEvent;
import com.zutubi.pulse.servercore.util.background.BackgroundServiceSupport;
import com.zutubi.util.logging.Logger;

/**
 * A simple service to farm assigned recipes out to the agents.  This is done
 * in a service as it requires a (possibly-slow) network call to the agent.
 */
public class RecipeDispatchService extends BackgroundServiceSupport
{
    private static final Logger LOG = Logger.getLogger(RecipeDispatchService.class);

    private EventManager eventManager;

    public RecipeDispatchService()
    {
        super("Recipe Dispatch");
    }

    public void dispatch(final RecipeAssignedEvent assignment)
    {
        getExecutorService().execute(new Runnable()
        {
            public void run()
            {
                try
                {
                    assignment.getAgent().getService().build(assignment.getRequest());
                    eventManager.publish(new RecipeDispatchedEvent(this, assignment.getRecipeId(), assignment.getAgent()));
                }
                catch (Exception e)
                {
                    LOG.warning("Unable to dispatch recipe: " + e.getMessage(), e);
                    eventManager.publish(new RecipeErrorEvent(this, assignment.getRecipeId(), "Unable to dispatch recipe: " + e.getMessage()));
                }
            }
        });
    }

    public void setEventManager(EventManager eventManager)
    {
        this.eventManager = eventManager;
    }
}