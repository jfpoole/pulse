package com.zutubi.pulse.events;

import com.zutubi.util.logging.Logger;

import java.util.List;

/**
 * An event dispatcher implementation that uses the thread on which the event was
 * published to dispatch to the listeners.
 */
public class SynchronousDispatcher implements EventDispatcher
{
    private static final Logger LOG = Logger.getLogger(SynchronousDispatcher.class);

    public void dispatch(Event evt, List<EventListener> listeners)
    {
        for (EventListener listener : listeners)
        {
            try
            {
                listener.handleEvent(evt);
            }
            catch (Exception e)
            {
                // isolate the exceptions generated by the event handling.
                evt.addException(e);
                LOG.severe("Exception generated by "+listener+".handleEvent("+evt+")", e);
            }
        }
    }
}
