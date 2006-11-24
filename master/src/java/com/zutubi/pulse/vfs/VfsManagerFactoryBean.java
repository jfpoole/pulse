package com.zutubi.pulse.vfs;

import com.zutubi.pulse.SlaveProxyFactory;
import com.zutubi.pulse.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.model.BuildManager;
import com.zutubi.pulse.model.ProjectManager;
import com.zutubi.pulse.model.SlaveManager;
import com.zutubi.pulse.search.Queries;
import com.zutubi.pulse.services.ServiceTokenManager;
import com.zutubi.pulse.util.logging.Logger;
import com.zutubi.pulse.vfs.agent.AgentFileProvider;
import com.zutubi.pulse.vfs.local.DefaultLocalFileProvider;
import com.zutubi.pulse.vfs.pulse.PulseFileProvider;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.impl.DefaultFileSystemManager;
import org.apache.commons.vfs.provider.ram.RamFileProvider;
import org.springframework.beans.factory.FactoryBean;

/**
 * <class comment/>
 */
public class VfsManagerFactoryBean implements FactoryBean
{
    private static final Logger LOG = Logger.getLogger(VfsManagerFactoryBean.class);

    private SlaveManager slaveManager;
    private SlaveProxyFactory proxyFactory;
    private ServiceTokenManager serviceTokenManager;
    private BuildManager buildManager;
    private MasterConfigurationManager configurationManager;
    private ProjectManager projectManager;
    private Queries queries;

    private DefaultFileSystemManager instance;

    public Object getObject() throws Exception
    {
        if (instance == null)
        {
            synchronized(this)
            {
                if (instance == null)
                {
                    instance = new DefaultFileSystemManager();
                    instance.addProvider("local", new DefaultLocalFileProvider());
                    instance.addProvider("ram", new RamFileProvider());

                    AgentFileProvider agentFileProviderfileProvider = new AgentFileProvider();
                    agentFileProviderfileProvider.setSlaveManager(slaveManager);
                    agentFileProviderfileProvider.setSlaveProxyFactory(proxyFactory);
                    agentFileProviderfileProvider.setServiceTokenManager(serviceTokenManager);
                    instance.addProvider("agent", agentFileProviderfileProvider);

                    PulseFileProvider pulseFileProvider = new PulseFileProvider();
                    pulseFileProvider.setBuildManager(buildManager);
                    pulseFileProvider.setProjectManager(projectManager);
                    pulseFileProvider.setQueries(queries);
                    pulseFileProvider.setConfigurationManager(configurationManager);
                    instance.addProvider("pulse", pulseFileProvider);

                    instance.init();
                }
            }
        }
        return instance;
    }

    public Class getObjectType()
    {
        return FileSystemManager.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void shutdown()
    {
        if (instance != null)
        {
            instance.close();
        }
    }

    public void setSlaveProxyFactory(SlaveProxyFactory proxyFactory)
    {
        this.proxyFactory = proxyFactory;
    }

    public void setSlaveManager(SlaveManager slaveManager)
    {
        this.slaveManager = slaveManager;
    }

    public void setServiceTokenManager(ServiceTokenManager serviceTokenManager)
    {
        this.serviceTokenManager = serviceTokenManager;
    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void setProjectManager(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    public void setQueries(Queries queries)
    {
        this.queries = queries;
    }
}
