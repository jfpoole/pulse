package com.zutubi.pulse.core.scm.git.config;

import com.zutubi.pulse.core.scm.api.ScmClientFactory;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.pulse.core.scm.git.GitClient;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.annotations.Wire;
import com.zutubi.tove.config.api.AbstractConfigurationCheckHandler;

/**
 * not yet implemented
 */
@Wire
@SymbolicName("zutubi.gitConfigurationCheckHandler")
public class GitConfigurationCheckHandler extends AbstractConfigurationCheckHandler<GitConfiguration>
{
    private ScmClientFactory<? super GitConfiguration> scmClientFactory;

    public void test(GitConfiguration configuration) throws ScmException
    {
        GitClient client = null;
        try
        {
            client = (GitClient) scmClientFactory.createClient(configuration);
            // can check the repository details by creating a local (no checkout) clone.
            // can check for the existance of the specified branch.
            // - local clone of repository
            // - list the remote branches, git remote show origin
        }
        finally
        {
            if (client != null)
            {
                client.close();
            }
        }
    }

    public void setScmClientFactory(ScmClientFactory<? super GitConfiguration> scmClientManager)
    {
        this.scmClientFactory = scmClientManager;
    }
}
