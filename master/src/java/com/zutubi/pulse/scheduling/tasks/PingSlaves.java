package com.zutubi.pulse.scheduling.tasks;

import com.zutubi.pulse.agent.AgentManager;
import com.zutubi.pulse.scheduling.Task;
import com.zutubi.pulse.scheduling.TaskExecutionContext;
import com.zutubi.pulse.util.logging.Logger;

/**
 * <class-comment/>
 */
public class PingSlaves implements Task
{
    private static final Logger LOG = Logger.getLogger(PingSlaves.class);

    private AgentManager agentManager;

    public void execute(TaskExecutionContext context)
    {
        LOG.info("pinging slaves.");
        agentManager.pingSlaves();
    }

    public void setAgentManager(AgentManager agentManager)
    {
        this.agentManager = agentManager;
    }
}