package com.zutubi.pulse.upgrade.tasks;

import com.zutubi.pulse.Version;
import com.zutubi.pulse.bootstrap.Data;
import com.zutubi.pulse.upgrade.UpgradeContext;
import com.zutubi.pulse.upgrade.UpgradeTask;

import java.util.List;

/**
 * <class-comment/>
 */
public class MockUpgradeContext implements UpgradeContext
{
    private int fromBuild;
    private int toBuild;

    public int getFromBuild()
    {
        return fromBuild;
    }

    public void setFromBuild(int fromBuild)
    {
        this.fromBuild = fromBuild;
    }

    public int getToBuild()
    {
        return toBuild;
    }

    public void setToBuild(int toBuild)
    {
        this.toBuild = toBuild;
    }

    public List<UpgradeTask> getTasks()
    {
        return null;
    }

    public Data getData()
    {
        return null;
    }

    public Version getFrom()
    {
        return null;
    }

    public Version getTo()
    {
        return null;
    }
}