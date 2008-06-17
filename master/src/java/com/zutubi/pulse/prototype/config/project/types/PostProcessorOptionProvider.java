package com.zutubi.pulse.prototype.config.project.types;

import com.zutubi.prototype.MapOption;
import com.zutubi.prototype.MapOptionProvider;
import com.zutubi.prototype.type.TypeProperty;
import com.zutubi.pulse.PostProcessorManager;

import java.util.Map;
import java.util.TreeMap;

/**
 * Provides options for selection of post-processors for output or artifacts.
 * TODO: it would be better to filter this list based on what is being configured
 */
public class PostProcessorOptionProvider extends MapOptionProvider
{
    private PostProcessorManager postProcessorManager;

    public MapOption getEmptyOption(Object instance, String parentPath, TypeProperty property)
    {
        return new MapOption("", "");
    }

    protected Map<String, String> getMap(Object instance, String parentPath, TypeProperty property)
    {
        Map<String, PostProcessorFragment> processors = postProcessorManager.getAvailableProcessors();
        Map<String, String> result = new TreeMap<String, String>();
        for(Map.Entry<String, PostProcessorFragment> entry: processors.entrySet())
        {
            result.put(entry.getKey(), entry.getValue().getDisplayName());
        }
        return result;
    }

    public void setPostProcessorManager(PostProcessorManager postProcessorManager)
    {
        this.postProcessorManager = postProcessorManager;
    }
}
