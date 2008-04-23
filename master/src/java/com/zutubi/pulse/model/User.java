package com.zutubi.pulse.model;

import com.zutubi.pulse.core.model.Entity;
import com.zutubi.pulse.prototype.config.user.UserConfiguration;
import com.zutubi.pulse.prototype.config.user.UserPreferencesConfiguration;

/**
 * 
 *
 */
public class User extends Entity
{
    public static int REFRESH_DISABLED = 0;

    /**
     * Indicates whether or not the user is enabled. Only enabled users can
     * log in to the system.
     */
    private boolean enabled = true;
    private long nextBuildNumber = 1;
    private UserConfiguration config;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public long getNextBuildNumber()
    {
        return nextBuildNumber;
    }

    public void setNextBuildNumber(long nextBuildNumber)
    {
        this.nextBuildNumber = nextBuildNumber;
    }

    public UserConfiguration getConfig()
    {
        return config;
    }

    public void setConfig(UserConfiguration config)
    {
        this.config = config;
    }

    public UserPreferencesConfiguration getPreferences()
    {
        if (config == null)
        {
            return null;
        }
        else
        {
            return config.getPreferences();
        }
    }
}
