package com.zutubi.pulse.agent;

import com.zutubi.pulse.BuildService;
import com.zutubi.pulse.SystemInfo;
import com.zutubi.pulse.logging.CustomLogRecord;
import com.zutubi.pulse.model.Slave;
import com.zutubi.pulse.services.ServiceTokenManager;
import com.zutubi.pulse.services.SlaveService;
import com.zutubi.pulse.services.SlaveStatus;
import com.zutubi.pulse.services.UpgradeState;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 */
public class SlaveAgent implements Agent
{
    /**
     * This cached version of the slave is fine for read-only purposes as a
     * new SlaveAgent is created on any change to the underlying Slave.
     * However, do not provide access to this outside this class as it should
     * not be used to update the slave (a new lookup should be forced).
     */
    private Slave slave;
    private Status status;
    private long lastPingTime = 0;
    private long recipeId = -1;
    private SlaveService slaveService;
    private ServiceTokenManager serviceTokenManager;
    private BuildService buildService;
    private String pingError = null;
    /**
     * The upgrade state is only used when the slave enable state is UPGRADING.
     */
    private UpgradeState upgradeState = UpgradeState.INITIAL;
    private int upgradeProgress = -1;
    private String upgradeMessage = null;

    public SlaveAgent(Slave slave, SlaveService slaveService, ServiceTokenManager serviceTokenManager, BuildService buildService)
    {
        this.slave = slave;
        this.slaveService = slaveService;
        this.serviceTokenManager = serviceTokenManager;
        this.buildService = buildService;

        // Restore transient state based on persistent state
        switch(slave.getEnableState())
        {
            case ENABLED:
                status = Status.INITIAL;
                break;
            case DISABLED:
            case DISABLING:
            case UPGRADING:
                status = Status.DISABLED;
                break;
            case FAILED_UPGRADE:
                status = Status.DISABLED;
                upgradeState = UpgradeState.FAILED;
                break;
        }
    }

    public long getId()
    {
        return slave.getId();
    }

    public BuildService getBuildService()
    {
        return buildService;
    }

    public SystemInfo getSystemInfo()
    {
        return slaveService.getSystemInfo(serviceTokenManager.getToken());
    }

    public List<CustomLogRecord> getRecentMessages()
    {
        return slaveService.getRecentMessages(serviceTokenManager.getToken());
    }

    public String getName()
    {
        return slave.getName();
    }

    public SlaveService getSlaveService()
    {
        return slaveService;
    }

    public Status getStatus()
    {
        return status;
    }

    public String getLocation()
    {
        return slave.getHost() + ":" + slave.getPort();
    }

    public boolean isSlave()
    {
        return true;
    }

    public long getLastPingTime()
    {
        return lastPingTime;
    }

    public boolean hasBeenPinged()
    {
        return lastPingTime != 0;
    }

    public String getPrettyPingTime()
    {
        if (hasBeenPinged())
        {
            return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date(lastPingTime));
        }
        else
        {
            return "<never>";
        }
    }

    public long getSecondsSincePing()
    {
        return (System.currentTimeMillis() - lastPingTime) / 1000;
    }

    private void setLastPingTime(long time)
    {
        lastPingTime = time;
    }

    public long getRecipeId()
    {
        return recipeId;
    }

    public void setRecipeId(long recipeId)
    {
        this.recipeId = recipeId;
    }

    public String getPingError()
    {
        return pingError;
    }

    public boolean isOnline()
    {
        return status.isOnline();
    }

    public boolean isEnabled()
    {
        return slave.isEnabled();
    }

    public boolean isDisabling()
    {
        return slave.getEnableState() == Slave.EnableState.DISABLING;
    }

    public boolean isUpgrading()
    {
        return slave.getEnableState() == Slave.EnableState.UPGRADING;
    }

    public boolean isFailedUpgrade()
    {
        return slave.getEnableState() == Slave.EnableState.FAILED_UPGRADE;
    }

    public boolean isAvailable()
    {
        return status == Status.IDLE;
    }

    public void updateStatus(SlaveStatus status)
    {
        updateStatus(Status.valueOf(status.getStatus().name()), status.getRecipeId(), status.getMessage());
    }

    public void updateStatus(Status status)
    {
        updateStatus(status, -1);
    }

    public void updateStatus(Status status, long recipeId)
    {
        updateStatus(status, recipeId, null);
    }

    public void updateStatus(Status status, long recipeId, String pingError)
    {
        lastPingTime = System.currentTimeMillis();
        this.status = status;
        this.recipeId = recipeId;
        this.pingError = pingError;
    }

    public void upgradeStatus(UpgradeState state, int progress, String message)
    {
        upgradeState = state;
        upgradeProgress = progress;
        upgradeMessage = message;
    }

    public void copyStatus(SlaveAgent existingAgent)
    {
        status = existingAgent.status;
        lastPingTime = existingAgent.lastPingTime;
        recipeId = existingAgent.recipeId;
        pingError = existingAgent.pingError;
        upgradeState = existingAgent.upgradeState;
        upgradeProgress = existingAgent.upgradeProgress;
        upgradeMessage = existingAgent.upgradeMessage;
    }

    public UpgradeState getUpgradeState()
    {
        return upgradeState;
    }

    public int getUpgradeProgress()
    {
        return upgradeProgress;
    }

    public String getUpgradeMessage()
    {
        return upgradeMessage;
    }

    public Slave.EnableState getEnableState()
    {
        return slave.getEnableState();
    }

    public void setSlave(Slave slave)
    {
        this.slave = slave;
    }
}