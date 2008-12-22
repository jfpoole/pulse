package com.zutubi.pulse.web.agents;

import com.zutubi.pulse.logging.CustomLogRecord;
import com.zutubi.pulse.web.ActionSupport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

/**
 * Helper base class for actions that display server messages.
 */
public class ServerMessagesActionSupport extends AgentActionSupport
{
    public boolean isError(CustomLogRecord record)
    {
        return record.getLevel().intValue() == Level.SEVERE.intValue();
    }

    public boolean isWarning(CustomLogRecord record)
    {
        return record.getLevel().intValue() == Level.WARNING.intValue();
    }

    public boolean hasThrowable(CustomLogRecord record)
    {
        return record.getStackTrace().length() > 0;
    }


}