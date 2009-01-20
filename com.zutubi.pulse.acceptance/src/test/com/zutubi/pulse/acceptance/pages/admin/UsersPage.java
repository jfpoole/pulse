package com.zutubi.pulse.acceptance.pages.admin;

import com.thoughtworks.selenium.Selenium;
import com.zutubi.pulse.master.webwork.Urls;
import com.zutubi.pulse.master.tove.config.ConfigurationRegistry;

/**
 * Simple specialisation of a list page for the admin/users tab.
 */
public class UsersPage extends ListPage
{
    public UsersPage(Selenium selenium, Urls urls)
    {
        super(selenium, urls, ConfigurationRegistry.USERS_SCOPE);
    }
}