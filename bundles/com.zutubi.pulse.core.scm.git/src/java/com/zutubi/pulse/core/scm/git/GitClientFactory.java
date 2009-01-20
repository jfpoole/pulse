package com.zutubi.pulse.core.scm.git;

import com.zutubi.pulse.core.scm.api.ScmClientFactory;
import com.zutubi.pulse.core.scm.api.ScmClient;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.pulse.core.scm.git.config.GitConfiguration;

/**
 * Implementation of the {@link com.zutubi.pulse.core.scm.api.ScmClientFactory} to handle the
 * creation of the Git ScmClient.
 *
 * @see com.zutubi.pulse.core.scm.git.GitClient
 */
public class GitClientFactory implements ScmClientFactory<GitConfiguration>
{
    public ScmClient createClient(GitConfiguration config) throws ScmException
    {
        return new GitClient(config.getRepository(), config.getBranch());
    }
}