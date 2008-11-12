package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.pages.server.ServerActivityPage;
import com.zutubi.pulse.acceptance.pages.server.ServerInfoPage;
import com.zutubi.pulse.acceptance.pages.server.ServerMessagesPage;

/**
 * Acceptance tests for the server section of the reporting UI.
 */
public class ServerSectionAcceptanceTest extends SeleniumTestBase
{
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testServerActivity() throws Exception
    {
        loginAsAdmin();
        ServerActivityPage page = new ServerActivityPage(selenium, urls);
        page.goTo();

        assertTextPresent("build queue");
        assertTextPresent("active builds");
        assertTextPresent("recipe queue");
    }

    public void testServerMessages() throws Exception
    {
        loginAsAdmin();
        ServerMessagesPage page = new ServerMessagesPage(selenium, urls);
        page.goTo();

        assertTextPresent("messages found");
    }

    public void testServerMessagesPaging() throws Exception
    {
        xmlRpcHelper.loginAsAdmin();
        try
        {
            for (int i = 0; i < 100; i++)
            {
                xmlRpcHelper.logError("Test error message " + i);
            }
        }
        finally
        {
            xmlRpcHelper.logout();
        }

        loginAsAdmin();
        ServerMessagesPage page = new ServerMessagesPage(selenium, urls);
        page.goTo();

        assertEquals("100 messages found", page.getMessagesCountText());
        page.assertPagingLinks(10);
        page = page.clickPage(5);
        page.waitFor();
        page.assertPagingLinks(10);
        assertTextPresent("Test error message 59");
    }

    public void testServerInfo() throws Exception
    {
        loginAsAdmin();
        ServerInfoPage page = new ServerInfoPage(selenium, urls);
        page.goTo();

        assertTextPresent("system information");
        assertTextPresent("java vm");
        assertTextPresent("version information");
        assertTextPresent("pulse paths");
    }
}
