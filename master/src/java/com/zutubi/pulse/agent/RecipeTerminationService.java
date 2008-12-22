package com.zutubi.pulse.agent;

import com.zutubi.pulse.events.Event;
import com.zutubi.pulse.events.EventListener;
import com.zutubi.pulse.events.EventManager;
import com.zutubi.pulse.events.build.RecipeTerminateRequestEvent;
import com.zutubi.pulse.util.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple service for requesting recipes to terminate.  As this involves a
 * network call to the agent, it is not desirable to do it inline.
 */
public class RecipeTerminationService implements EventListener
{
    private static final Logger LOG = Logger.getLogger(RecipeTerminationService.class);

    private AtomicInteger id = new AtomicInteger(1);
    private ExecutorService executorService;
    private EventManager eventManager;

    public void init()
    {
        executorService = Executors.newCachedThreadPool(new ThreadFactory()
        {
            public Thread newThread(Runnable r)
            {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("Recipe Termination Service Worker " + id.getAndIncrement());
                return t;
            }
        });

        eventManager.register(this);
    }

    public void handleEvent(final Event e)
    {
        executorService.execute(new Runnable()
        {
            public void run()
            {
                RecipeTerminateRequestEvent rtr = (RecipeTerminateRequestEvent) e;
                try
                {
                    rtr.getService().terminateRecipe(rtr.getRecipeId());
                }
                catch (Exception e)
                {
                    LOG.warning("Unable to terminate recipe '" + rtr.getRecipeId() + "': " + e.getMessage(), e);
                }
            }
        });
    }

    public Class[] getHandledEvents()
    {
        return new Class[] { RecipeTerminateRequestEvent.class };
    }

    public void setEventManager(EventManager eventManager)
    {
        this.eventManager = eventManager;
    }
}