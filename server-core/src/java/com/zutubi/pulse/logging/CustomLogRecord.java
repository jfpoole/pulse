package com.zutubi.pulse.logging;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 */
public class CustomLogRecord
{
    private Level level;
    private String loggerName;
    private long sequenceNumber;
    private String sourceClassName;
    private String sourceMethodName;
    private String message;
    private int threadId;
    private long millis;
    private String stackTrace;

    private int count = 1;

    public CustomLogRecord()
    {
    }

    public CustomLogRecord(LogRecord logRecord)
    {
        this.level = logRecord.getLevel();
        this.loggerName = logRecord.getLoggerName();
        this.sequenceNumber = logRecord.getSequenceNumber();
        this.sourceClassName = logRecord.getSourceClassName();
        this.sourceMethodName = logRecord.getSourceMethodName();
        this.message = logRecord.getMessage();
        this.threadId = logRecord.getThreadID();
        this.millis = logRecord.getMillis();
        this.stackTrace = getStackTrace(logRecord);
    }

    public static String getStackTrace(LogRecord record)
    {
        Throwable t = record.getThrown();
        if (t != null)
        {
            StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            return writer.getBuffer().toString();
        }
        else
        {
            return "";
        }
    }

    public Level getLevel()
    {
        return level;
    }

    public String getLoggerName()
    {
        return loggerName;
    }

    public long getSequenceNumber()
    {
        return sequenceNumber;
    }

    public String getSourceClassName()
    {
        return sourceClassName;
    }

    public String getSourceMethodName()
    {
        return sourceMethodName;
    }

    public String getMessage()
    {
        return message;
    }

    public int getThreadId()
    {
        return threadId;
    }

    public long getMillis()
    {
        return millis;
    }

    public String getStackTrace()
    {
        return stackTrace;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    public void repeated()
    {
        count++;
    }
}
