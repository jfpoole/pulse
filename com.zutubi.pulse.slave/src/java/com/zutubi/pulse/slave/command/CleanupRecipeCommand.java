package com.zutubi.pulse.slave.command;

import com.zutubi.pulse.ServerRecipePaths;
import com.zutubi.pulse.core.util.FileSystemUtils;
import com.zutubi.pulse.bootstrap.ConfigurationManager;
import com.zutubi.util.logging.Logger;

import java.io.File;

/**
 */
public class CleanupRecipeCommand implements Runnable
{
    private static final Logger LOG = Logger.getLogger(CleanupRecipeCommand.class);

    private String project;
    private long recipeId;
    private boolean incremental;
    private ConfigurationManager configurationManager;

    public CleanupRecipeCommand(String project, long recipeId, boolean incremental)
    {
        this.project = project;
        this.recipeId = recipeId;
        this.incremental = incremental;
    }

    public void run()
    {
        ServerRecipePaths recipeProcessorPaths = new ServerRecipePaths(project, recipeId, configurationManager.getUserPaths().getData(), incremental);
        File recipeRoot = recipeProcessorPaths.getRecipeRoot();
        if (!FileSystemUtils.rmdir(recipeRoot))
        {
            LOG.warning("Unable to remove recipe directory '" + recipeRoot + "'");
        }
    }

    public void setConfigurationManager(ConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }
}
