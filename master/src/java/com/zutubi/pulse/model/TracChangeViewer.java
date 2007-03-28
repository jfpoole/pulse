package com.zutubi.pulse.model;

import com.zutubi.pulse.core.model.FileRevision;
import com.zutubi.pulse.core.model.Revision;
import com.zutubi.pulse.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * A change viwer implementation for linking to a Trac instance.
 */
public class TracChangeViewer extends BasePathChangeViewer
{
    private TracChangeViewer()
    {
        super(null, null);
    }

    public TracChangeViewer(String baseURL, String projectPath)
    {
        super(baseURL, projectPath);
    }

    public String getDetails()
    {
        return "Trac [" + getBaseURL() + "]";
    }

    public String getChangesetURL(Revision revision)
    {
        return StringUtils.join("/", true, true, getBaseURL(), "changeset", revision.getRevisionString());
    }

    public String getFileViewURL(String path, FileRevision revision)
    {
        return StringUtils.join("/", true, true, getBaseURL(), "browser", StringUtils.urlEncodePath(path) + "?rev=" + revision.getRevisionString());
    }

    public String getFileDownloadURL(String path, FileRevision revision)
    {
        return getFileViewURL(path, revision) + "&format=raw";
    }

    public String getFileDiffURL(String path, FileRevision revision)
    {
        FileRevision previous = revision.getPrevious();
        if(previous == null)
        {
            return null;
        }

        return StringUtils.join("/", true, true, getBaseURL(), "changeset?new=" + getDiffPath(path, revision) + "&old=" + getDiffPath(path, previous));
    }

    public ChangeViewer copy()
    {
        return new TracChangeViewer(getBaseURL(), getProjectPath());
    }

    private String getDiffPath(String path, FileRevision revision)
    {
        String result = StringUtils.join("/", path + "@" + revision.getRevisionString());
        if(result.startsWith("/"))
        {
            result = result.substring(1);
        }
        
        try
        {
            return URLEncoder.encode(result, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            // Programmer error!
            return result;
        }
    }
}
