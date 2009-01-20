package com.zutubi.pulse.core.model;

import com.zutubi.pulse.core.postprocessors.api.Feature;

/**
 * A PlainFeature is a feture discovered by analysing an artifact as plain
 * text, line by line.  In addition to the summary, these features contain
 * location information.
 */
public class PersistentPlainFeature extends PersistentFeature
{
    /**
     * One-based line number of where this feature (possibly including
     * context) starts (inclusive).
     */
    private long firstLine;
    /**
     * One-based line number of where this feature (possibly including
     * context) ends (inclusive).
     */
    private long lastLine;
    /**
     * One-based line number of the line that contains the actual detected
     * feature.
     */
    private long lineNumber;


    public PersistentPlainFeature()
    {

    }

    public PersistentPlainFeature(Feature.Level level, String summary, long lineNumber)
    {
        super(level, summary);

        this.lineNumber = lineNumber;
        this.firstLine = lineNumber;
        this.lastLine = lineNumber;
    }

    public PersistentPlainFeature(Feature.Level level, String summary, long firstLine, long lastLine, long lineNumber)
    {
        super(level, summary);

        this.firstLine = firstLine;
        this.lastLine = lastLine;
        this.lineNumber = lineNumber;
    }

    public long getFirstLine()
    {
        return firstLine;
    }

    public void setFirstLine(long firstLine)
    {
        this.firstLine = firstLine;
    }

    public long getLastLine()
    {
        return lastLine;
    }

    public void setLastLine(long lastLine)
    {
        this.lastLine = lastLine;
    }

    public long getLineNumber()
    {
        return lineNumber;
    }

    private void setLineNumber(long line)
    {
        lineNumber = line;
    }

    public boolean isPlain()
    {
        return true;
    }

    public boolean hasLeadingContext()
    {
        return lineNumber > firstLine;
    }

    public boolean hasTrailingContext()
    {
        return lineNumber < lastLine;
    }

    public boolean hasContext()
    {
        return hasLeadingContext() || hasTrailingContext();
    }

    public int lineOffset()
    {
        return (int) (lineNumber - firstLine + 1);
    }

    public String[] getSummaryLines()
    {
        return getSummary().split("\n");
    }

    public boolean equals(Object o)
    {
        if (o instanceof PersistentPlainFeature)
        {
            PersistentPlainFeature other = (PersistentPlainFeature) o;
            return super.equals(o) && other.firstLine == firstLine && other.lineNumber == lineNumber && other.lastLine == lastLine;
        }

        return super.equals(o);
    }
}