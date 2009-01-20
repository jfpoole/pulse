package com.zutubi.pulse.master.xwork.actions.project;

import java.io.File;

/**
 */
public class ViewArtifactsAction extends CommandActionBase
{
    public String getSeparator()
    {
        return File.separator.replace("\\", "\\\\");
    }

    public String execute()
    {
        // Optional discovery down to the command level.
        getCommandResult();
        // We require at least down to the build level
        getRequiredBuildResult();
        return SUCCESS;
    }
}