package com.zutubi.pulse.agent;

import com.zutubi.pulse.BuildService;
import com.zutubi.pulse.MasterBuildService;
import com.zutubi.pulse.SystemInfo;
import com.zutubi.pulse.logging.CustomLogRecord;
import com.zutubi.pulse.logging.ServerMessagesHandler;
import com.zutubi.pulse.bootstrap.ConfigurationManager;
import com.zutubi.pulse.bootstrap.StartupManager;

import java.util.List;

/**
 */
public class MasterAgent implements Agent
{
    private MasterBuildService service;
    private ConfigurationManager configurationManager;
    private StartupManager startupManager;
    private ServerMessagesHandler serverMessagesHandler;

    public MasterAgent(MasterBuildService service, ConfigurationManager configurationManager, StartupManager startupManager, ServerMessagesHandler serverMessagesHandler)
    {
        this.service = service;
        this.configurationManager = configurationManager;
        this.startupManager = startupManager;
        this.serverMessagesHandler = serverMessagesHandler;
    }

    public long getId()
    {
        return 0;
    }

    public BuildService getBuildService()
    {
        return service;
    }

    public SystemInfo getSystemInfo()
    {
        return SystemInfo.getSystemInfo(configurationManager, startupManager);
    }

    public List<CustomLogRecord> getRecentMessages()
    {
        return serverMessagesHandler.takeSnapshot();
    }

    public Status getStatus()
    {
        return Status.IDLE;
    }

    public String getLocation()
    {
        return configurationManager.getAppConfig().getHostName();
    }

    public boolean isSlave()
    {
        return false;
    }

    public String getName()
    {
        return "[master]";
    }
}
