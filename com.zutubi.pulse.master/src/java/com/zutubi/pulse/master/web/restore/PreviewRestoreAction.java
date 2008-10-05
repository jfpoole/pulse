package com.zutubi.pulse.master.web.restore;

import com.zutubi.pulse.master.util.monitor.Task;
import com.zutubi.pulse.master.restore.Archive;

import java.util.List;

/**
 *
 *
 */
public class PreviewRestoreAction extends RestoreActionSupport
{
    private Archive archive;

    public List<Task> getTasks()
    {
        return restoreManager.previewRestore();
    }

    public Archive getInfo()
    {
        return archive;
    }

    public String execute() throws Exception
    {
        archive = restoreManager.getArchive();

        return SUCCESS;
    }
}
