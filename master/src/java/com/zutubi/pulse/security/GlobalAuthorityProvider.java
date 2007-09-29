package com.zutubi.pulse.security;

import com.zutubi.prototype.config.ConfigurationTemplateManager;
import com.zutubi.prototype.security.AuthorityProvider;
import com.zutubi.prototype.security.DefaultAccessManager;
import com.zutubi.pulse.model.GrantedAuthority;
import com.zutubi.pulse.prototype.config.admin.GlobalConfiguration;
import com.zutubi.pulse.prototype.config.group.ServerPermission;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides allowed authorities for global (i.e. server-wide) actions.
 */
public class GlobalAuthorityProvider implements AuthorityProvider<Object>
{
    public static final String CREATE_USER = "CREATE_USER";

    private ConfigurationTemplateManager configurationTemplateManager;

    public Set<String> getAllowedAuthorities(String action, Object resource)
    {
        Set<String> result = new HashSet<String>();
        result.add(action);

        if(CREATE_USER.equals(action))
        {
            GlobalConfiguration config = configurationTemplateManager.getInstance(GlobalConfiguration.SCOPE_NAME, GlobalConfiguration.class);
            if(config.getGeneralConfig().isAnonymousSignupEnabled())
            {
                result.add(GrantedAuthority.ANONYMOUS);
                result.add(GrantedAuthority.GUEST);
                result.add(GrantedAuthority.USER);
            }
        }

        return result;
    }

    public void setAccessManager(DefaultAccessManager accessManager)
    {
        accessManager.addSuperAuthority(ServerPermission.ADMINISTER.toString());
        accessManager.registerAuthorityProvider(this);
    }

    public void setConfigurationTemplateManager(ConfigurationTemplateManager configurationTemplateManager)
    {
        this.configurationTemplateManager = configurationTemplateManager;
    }
}
