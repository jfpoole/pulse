package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.forms.setup.*;
import com.zutubi.pulse.master.license.LicenseHelper;
import com.zutubi.pulse.master.license.LicenseType;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * A setup test that covers the systems setup procedure.
 * <p/>
 * This setup test is a little awkward since we can only run it once. Once done, the
 * server is setup and will not take kindly to us trying to set it up again. So, rather than
 * having multiple test methods, there is one testSetupProcess method that is breaks up the setup
 * process and handles all of the validation testing as it goes.
 */
public class SetupAcceptanceTest extends SeleniumTestBase
{
    public String licenseKey;
    public String expiredLicenseKey;
    public String invalidLicenseKey;

    protected void setUp() throws Exception
    {
        super.setUp();

        licenseKey = LicenseHelper.newLicenseKey(LicenseType.EVALUATION, "S. O. MeBody");
        expiredLicenseKey = LicenseHelper.newExpiredLicenseKey(LicenseType.EVALUATION, "S. O. MeBody");
        invalidLicenseKey = LicenseHelper.newInvalidLicenseKey(LicenseType.EVALUATION, "S. O. MeBody");
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testSetupProcess() throws InterruptedException, IOException, SAXException
    {
        // first we deal with the pulse home property configuration.
        goTo("setup/setupData!input.action");

        // step one. setting the pulse home variable.
        checkSetPulseData();

        _checkPostPulseData();

        // lets also take this opportunity to verify that what was set during the setup wizard
        // was actually persisted.
    }


    protected void checkPostPulseData()
    {
        goTo("/");
        _checkPostPulseData();
    }

    protected void _checkPostPulseData()
    {
        // step two. setting up the database
        checkSetupDatabase();

        // step three. setting the license details.
        checkLicenseDetails();

        // step four. creating the administration user.
        checkCreateAdmin();

        // step five. configuring the server essentials.
        checkServerSettings();

        assertTextPresent("system setup");
        waitForElement("welcome.heading", 60000);

        // one complete, we should see the home page, and it should contain the following:
        assertTextPresent(":: welcome ::");
        assertTextPresent("A. D. Ministrator");
        assertLinkPresent("logout");
    }

    private void checkSetPulseData()
    {
        assertPulseTabsNotVisible();

        SetPulseDataForm form = new SetPulseDataForm(selenium);
        assertTrue(form.isFormPresent());
        assertFormFieldNotEmpty("zfid.data");

        String defaultDataDir = "data";

        form.nextFormElements("");
        assertTextPresent("pulse data directory requires a value");
        assertTrue(form.isFormPresent());

        form.nextFormElements(defaultDataDir);
    }

    private void assertPulseTabsNotVisible()
    {
        assertElementNotPresent("tab.projects");
        assertElementNotPresent("tab.queues");
        assertElementNotPresent("tab.agents");
        assertElementNotPresent("tab.administration");
    }

    private void checkSetupDatabase()
    {
        assertPulseTabsNotVisible();

        SetupDatabaseTypeForm form = new SetupDatabaseTypeForm(selenium);
        assertTrue(form.isFormPresent());
        form.nextFormElements("EMBEDDED", null, null, null, null, null, null);
    }

    private void checkLicenseDetails()
    {
        assertPulseTabsNotVisible();

        PulseLicenseForm licenseForm = new PulseLicenseForm(selenium);

        assertTrue(licenseForm.isFormPresent());

        // check that license is required.
        licenseForm.nextFormElements("");
        assertTrue(licenseForm.isFormPresent());
        assertFormElements(licenseForm, "");
        assertTextPresent("license key requires a value");

        // check that license validation works.
        licenseForm.nextFormElements(invalidLicenseKey);
        assertTrue(licenseForm.isFormPresent());
        assertFormElements(licenseForm, invalidLicenseKey);
        assertTextPresent("invalid");

        // check that an expired license is not accepted.
        licenseForm.nextFormElements(expiredLicenseKey);
        assertTrue(licenseForm.isFormPresent());
        assertFormElements(licenseForm, expiredLicenseKey);
        assertTextPresent("expired");

        // enter a valid license.
        licenseForm.nextFormElements(licenseKey);
        assertFalse(licenseForm.isFormPresent());
    }

    private void checkCreateAdmin()
    {
        assertPulseTabsNotVisible();

        CreateAdminForm createAdminForm = new CreateAdminForm(selenium);

        // create admin.
        assertTrue(createAdminForm.isFormPresent());
        createAdminForm.nextFormElements("admin", "A. D. Ministrator", "admin", "admin");
    }

    private void checkServerSettings()
    {
        assertPulseTabsNotVisible();

        ServerSettingsForm settingsForm = new ServerSettingsForm(selenium);
        assertTrue(settingsForm.isFormPresent());
        settingsForm.finishFormElements("http://localhost:8080", "some.smtp.host.com", "true", "Setup <from@localhost.com>", "username", "password", "prefix", "true", "123");
    }

}