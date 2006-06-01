package com.zutubi.pulse.agent;

import com.zutubi.pulse.model.Slave;

import java.util.List;

/**
 */
public interface AgentManager
{
    List<Agent> getAllAgents();
    List<Agent> getOnlineAgents();
    Agent getAgent(Slave slave);

    public void pingSlaves();

    public void newSlave(long id);
    public void slaveChanged(long id);
}
