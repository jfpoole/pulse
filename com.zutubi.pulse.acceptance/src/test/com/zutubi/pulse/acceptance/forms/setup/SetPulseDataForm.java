package com.zutubi.pulse.acceptance.forms.setup;

import com.thoughtworks.selenium.Selenium;
import com.zutubi.pulse.acceptance.forms.SeleniumForm;
import com.zutubi.pulse.acceptance.SeleniumBrowser;
import com.zutubi.pulse.acceptance.windows.PulseFileSystemBrowserWindow;
import com.zutubi.pulse.master.tove.config.setup.SetupDataConfiguration;

public class SetPulseDataForm extends SeleniumForm
{
    private static final String FIELD_DATA = "data";

    public SetPulseDataForm(SeleniumBrowser browser)
    {
        super(browser, false);
    }

    public String getFormName()
    {
        return SetupDataConfiguration.class.getName();
    }

    public String[] getFieldNames()
    {
        return new String[]{FIELD_DATA};
    }

    public boolean isBrowseDataLinkPresent()
    {
        return browser.isElementIdPresent(getBrowseLinkId(FIELD_DATA));
    }

    public PulseFileSystemBrowserWindow clickBrowseData()
    {
        browser.click(getBrowseLinkId(FIELD_DATA));
        return new PulseFileSystemBrowserWindow(browser);
    }

    private String getBrowseLinkId(String field)
    {
        return getFieldId(field) + ".browse";
    }
}
