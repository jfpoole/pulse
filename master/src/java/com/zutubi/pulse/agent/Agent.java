package com.zutubi.pulse.agent;

import com.zutubi.pulse.AgentService;
import com.zutubi.pulse.model.AgentState;
import com.zutubi.pulse.services.SlaveStatus;
import com.zutubi.pulse.services.UpgradeState;
import com.zutubi.pulse.tove.config.agent.AgentConfiguration;

/**
 * An abstraction over an agent, be it the local (master) agent or a slave.
 */
public interface Agent
{
    Status getStatus();
    String getLocation();

    long getId();

    void updateStatus(SlaveStatus status);
    void updateStatus(Status status);
    void updateStatus(Status status, long recipeId);

    void copyStatus(Agent agent);

    void upgradeStatus(UpgradeState state, int progress, String message);

    AgentConfiguration getConfig();
    AgentService getService();

    long getSecondsSincePing();

    long getRecipeId();
    void setRecipeId(long recipeId);

    boolean isOnline();
    boolean isEnabled();
    boolean isDisabling();
    boolean isDisabled();
    boolean isUpgrading();
    boolean isFailedUpgrade();
    boolean isAvailable();

    AgentState.EnableState getEnableState();

    void setAgentState(AgentState agentState);
}
