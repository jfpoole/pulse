package com.zutubi.pulse.prototype.config.project.types;

/**
 * Implemented by objects that can provide a post-processor Pulse file
 * fragment.
 */
public interface PostProcessorFragment
{
    String getName();
    String getDisplayName();
    String getFragment();
}
