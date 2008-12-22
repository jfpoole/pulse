package com.zutubi.pulse.condition;

import com.zutubi.pulse.model.BuildResult;
import com.zutubi.pulse.model.User;

/**
 * Describes an interface for making notifications conditional based on
 * properties of the build model (e.g. only notify on build failed).
 *
 * @author jsankey
 */
public interface NotifyCondition
{
    public boolean satisfied(BuildResult result, User user);
}