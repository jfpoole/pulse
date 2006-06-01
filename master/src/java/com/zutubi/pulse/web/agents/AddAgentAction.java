/********************************************************************************
  @COPYRIGHT@
 ********************************************************************************/
package com.zutubi.pulse.web.agents;

import com.zutubi.pulse.model.Slave;
import com.zutubi.pulse.model.SlaveManager;
import com.zutubi.pulse.web.ActionSupport;
import com.zutubi.pulse.agent.AgentManager;

/**
 * 
 *
 */
public class AddAgentAction extends ActionSupport
{
    private Slave slave = new Slave();
    private AgentManager agentManager;
    private SlaveManager slaveManager;

    public Slave getSlave()
    {
        return slave;
    }

    public void validate()
    {
        if (hasErrors())
        {
            // do not attempt to validate unless all other validation rules have 
            // completed successfully.
            return;
        }

        if (slave.getName().equals("master") || slaveManager.getSlave(slave.getName()) != null)
        {
            // slave name already in use.
            addFieldError("slave.name", "An agent with name '" + slave.getName() + "' already exists.");
        }
    }

    public String execute()
    {
        slaveManager.save(slave);
        agentManager.newSlave(slave.getId());
        return SUCCESS;
    }

    public String doDefault()
    {
        // setup any default data.
        return SUCCESS;
    }

    public void setSlaveManager(SlaveManager slaveManager)
    {
        this.slaveManager = slaveManager;
    }

    public void setAgentManager(AgentManager agentManager)
    {
        this.agentManager = agentManager;
    }
}
