package com.cinnamonbob.event;

import com.cinnamonbob.events.Event;
import com.cinnamonbob.core.model.RecipeResult;

/**
 * An Event sent from slave to master, carrying info about the slave it
 * originated from and the actual event that occured as payload.
 */
public class SlaveEvent extends Event
{
    private String slave;
    private Event event;

    public SlaveEvent(Object source, String slave, RecipeResult result)
    {
        super(source);
        this.slave = slave;
    }

    public String getSlave()
    {
        return slave;
    }
}
