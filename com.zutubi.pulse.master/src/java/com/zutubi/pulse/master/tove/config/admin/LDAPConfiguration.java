package com.zutubi.pulse.master.tove.config.admin;

import com.zutubi.tove.annotations.*;
import com.zutubi.tove.config.api.AbstractConfiguration;
import com.zutubi.validation.annotations.Required;

/**
 * Configuration for LDAP auth integration.
 */
@SymbolicName("zutubi.ldapConfig")
@Form(fieldOrder = {"enabled", "ldapUrl", "baseDn", "managerDn", "managerPassword",
        "userBaseDn", "userFilter", "autoAddUsers", "emailAttribute", "groupBaseDn", "groupSearchFilter",
        "groupRoleAttribute", "searchGroupSubtree", "followReferrals", "escapeSpaceCharacters"})
@ConfigurationCheck("LDAPConfigurationCheckHandler")
@Classification(single = "security")
public class LDAPConfiguration extends AbstractConfiguration
{
    @ControllingCheckbox
    private boolean enabled;
    @Required
    private String ldapUrl;
    @Required
    private String baseDn;

    private String managerDn;
    @Password
    private String managerPassword;

    private String userBaseDn;
    @Required
    private String userFilter;
    private boolean autoAddUsers;
    private String emailAttribute;

    private String groupBaseDn;
    private String groupSearchFilter;
    private String groupRoleAttribute;
    private boolean searchGroupSubtree;

    private boolean escapeSpaceCharacters;
    private boolean followReferrals;

    public LDAPConfiguration()
    {
        setPermanent(true);
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getLdapUrl()
    {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl)
    {
        this.ldapUrl = ldapUrl;
    }

    public String getBaseDn()
    {
        return baseDn;
    }

    public void setBaseDn(String baseDn)
    {
        this.baseDn = baseDn;
    }

    public String getManagerDn()
    {
        return managerDn;
    }

    public void setManagerDn(String managerDn)
    {
        this.managerDn = managerDn;
    }

    public String getManagerPassword()
    {
        return managerPassword;
    }

    public void setManagerPassword(String managerPassword)
    {
        this.managerPassword = managerPassword;
    }

    public String getUserBaseDn()
    {
        return userBaseDn;
    }

    public void setUserBaseDn(String userBaseDn)
    {
        this.userBaseDn = userBaseDn;
    }

    public String getUserFilter()
    {
        return userFilter;
    }

    public void setUserFilter(String userFilter)
    {
        this.userFilter = userFilter;
    }

    public boolean getAutoAddUsers()
    {
        return autoAddUsers;
    }

    public void setAutoAddUsers(boolean autoAddUsers)
    {
        this.autoAddUsers = autoAddUsers;
    }

    public String getEmailAttribute()
    {
        return emailAttribute;
    }

    public void setEmailAttribute(String emailAttribute)
    {
        this.emailAttribute = emailAttribute;
    }

    public String getGroupBaseDn()
    {
        return groupBaseDn;
    }

    public void setGroupBaseDn(String groupBaseDn)
    {
        this.groupBaseDn = groupBaseDn;
    }

    public String getGroupSearchFilter()
    {
        return groupSearchFilter;
    }

    public void setGroupSearchFilter(String groupSearchFilter)
    {
        this.groupSearchFilter = groupSearchFilter;
    }

    public String getGroupRoleAttribute()
    {
        return groupRoleAttribute;
    }

    public void setGroupRoleAttribute(String groupRoleAttribute)
    {
        this.groupRoleAttribute = groupRoleAttribute;
    }

    public boolean getSearchGroupSubtree()
    {
        return searchGroupSubtree;
    }

    public void setSearchGroupSubtree(boolean searchGroupSubtree)
    {
        this.searchGroupSubtree = searchGroupSubtree;
    }

    public boolean getFollowReferrals()
    {
        return followReferrals;
    }

    public void setFollowReferrals(boolean followReferrals)
    {
        this.followReferrals = followReferrals;
    }

    public boolean getEscapeSpaceCharacters()
    {
        return escapeSpaceCharacters;
    }

    public void setEscapeSpaceCharacters(boolean escapeSpaceCharacters)
    {
        this.escapeSpaceCharacters = escapeSpaceCharacters;
    }
}