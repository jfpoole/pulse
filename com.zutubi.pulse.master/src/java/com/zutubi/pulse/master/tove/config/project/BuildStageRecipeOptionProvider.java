package com.zutubi.pulse.master.tove.config.project;

import com.zutubi.pulse.core.PulseFileLoader;
import com.zutubi.pulse.core.PulseFileLoaderFactory;
import com.zutubi.pulse.master.tove.handler.ListOptionProvider;
import com.zutubi.tove.config.ConfigurationProvider;
import com.zutubi.tove.config.api.Configuration;
import com.zutubi.tove.type.TypeProperty;
import com.zutubi.util.ConcurrentUtils;
import com.zutubi.util.logging.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * An option provider for the recipe field of a build stage.
 */
public class BuildStageRecipeOptionProvider extends ListOptionProvider
{
    private static final Logger LOG = Logger.getLogger(BuildStageRecipeOptionProvider.class);

    private static final int TIMEOUT_SECONDS = 10;
    
    private ConfigurationProvider configurationProvider;
    private PulseFileLoaderFactory fileLoaderFactory;

    public String getEmptyOption(Object instance, String parentPath, TypeProperty property)
    {
        return null;
    }

    public List<String> getOptions(Object instance, final String parentPath, TypeProperty property)
    {
        List<String> recipes = new LinkedList<String>();
        recipes.add("");

        try
        {
            List<String> recipeNames = ConcurrentUtils.runWithTimeout(new Callable<List<String>>()
            {
                public List<String> call() throws Exception
                {
                    Configuration stages = configurationProvider.get(parentPath, Configuration.class);
                    ProjectConfiguration projectConfig = configurationProvider.getAncestorOfType(stages, ProjectConfiguration.class);
                    if (projectConfig != null)
                    {
                        String pulseFileString = projectConfig.getType().getPulseFile(projectConfig, null, null);
                        PulseFileLoader pulseFileLoader = fileLoaderFactory.createLoader();
                        return pulseFileLoader.loadAvailableRecipes(pulseFileString);
                    }

                    return Collections.emptyList();
                }
            }, TIMEOUT_SECONDS, TimeUnit.SECONDS, Collections.<String>emptyList());

            for (String recipe: recipeNames)
            {
                recipes.add(recipe);
            }
        }
        catch (Exception e)
        {
            LOG.warning("Unable to load Pulse file to load available recipes: " + e.getMessage(), e);
        }

        return recipes;
    }

    public void setConfigurationProvider(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    public void setFileLoaderFactory(PulseFileLoaderFactory fileLoaderFactory)
    {
        this.fileLoaderFactory = fileLoaderFactory;
    }
}