package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.forms.*;
import com.zutubi.pulse.util.RandomUtils;
import net.sourceforge.jwebunit.ExpectedRow;
import net.sourceforge.jwebunit.ExpectedTable;

/**
 *
 *
 */
public class UserPreferencesAcceptanceTest extends BaseAcceptanceTestCase
{
    private String login;
    private static final String CONTACT_CREATE_TYPE = "contact";
    private static final String CREATE_CONTACT_LINK = "create contact";
    //TODO - replace this string with a reference to the properties file.
    private static final String CONTACT_REQUIRED = "you must create a contact point before you can create a subscription";
    private static final String JABBER_CONTACT = "myjabber";

    public UserPreferencesAcceptanceTest()
    {
    }

    public UserPreferencesAcceptanceTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        // create new user..
        loginAsAdmin();
        navigateToUserAdministration();
        login = RandomUtils.randomString(7);
        submitCreateUserForm(login, login, login, login, false);

        login(login, login);

        // navigate to the preferences tab.
        navigateToPreferences();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testUserPreferences()
    {
        // assert tabular data.
        assertTablePresent("user");
        assertTableRowsEqual("user", 1, new String[][]{
                new String[]{"login", login},   // login row
                new String[]{"name", login}     // name row
        });

        assertAliasesTable();

        assertSettingsTable("welcome", "every 60 seconds", "every 60 seconds");

        assertTablePresent("contacts");
        assertTableRowsEqual("contacts", 1, new String[][]{
                new String[]{"name", "uid", "actions"},
                new String[]{CREATE_CONTACT_LINK, CREATE_CONTACT_LINK, CREATE_CONTACT_LINK}
        });


        assertTablePresent("subscriptions");
        assertTableRowsEqual("subscriptions", 1, new String[][]{
                new String[]{"contact", "projects", "condition", "actions"},
                new String[]{CONTACT_REQUIRED, CONTACT_REQUIRED, CONTACT_REQUIRED, CONTACT_REQUIRED}
        });

        // can not create subscriptions unless there are projects to subscribe to.
        assertLinkNotPresentWithText("create subscription");
    }

    //---( test the email contact point )---
    public EmailContactForm emailSetup()
    {
        assertAndClick("contact.create");

        CreateContactForm contactForm = new CreateContactForm(tester);
        contactForm.assertFormPresent();
        assertOptionsEqual("contact", new String[]{ "email", "jabber" });
        contactForm.nextFormElements("email");

        EmailContactForm emailForm = new EmailContactForm(tester, true);
        emailForm.assertFormPresent();
        return emailForm;
    }

    public void testAddEmailContactValidation()
    {
        EmailContactForm emailForm = emailSetup();

        // name is required.
        emailForm.saveFormElements("", "email@example.com", "html");
        emailForm.assertFormPresent();
        emailForm.assertFormElements("", "email@example.com", "html");
        assertTextPresent("required");

        // email is required
        emailForm.saveFormElements("example", "", "plain");
        emailForm.assertFormPresent();
        emailForm.assertFormElements("example", "", "plain");
        assertTextPresent("required");

        // email must be valid
        emailForm.saveFormElements("example", "incorrect email address", "html");
        emailForm.assertFormPresent();
        emailForm.assertFormElements("example", "incorrect email address", "html");
        assertTextPresent("valid");

        // no need to check the radio box here.
    }

    public void testCancelEmailContact()
    {
        EmailContactForm emailForm = emailSetup();

        emailForm.cancelFormElements("example", "email@example.com", "plain");
        emailForm.assertFormNotPresent();

        assertTablePresent("contacts");
        assertTextNotPresent("example");
        assertTextNotPresent("email@example.com");
    }

    public void testEditUser()
    {
        // assert tabular data.
        assertTablePresent("user");
        assertTableRowsEqual("user", 1, new String[][]{
                new String[]{"login", login},   // login row
                new String[]{"name", login}     // name row
        });

        assertLinkPresent("user.edit");
        clickLink("user.edit");

        EditUserForm form = new EditUserForm(tester);
        form.assertFormElements(login);
        form.saveFormElements("S. O. MeBody");

        assertTablePresent("user");
        assertTableRowsEqual("user", 1, new String[][]{
                new String[]{"login", login},   // login row
                new String[]{"name", "S. O. MeBody"}     // name row
        });
    }

    public void testCancelEditUser()
    {
        // assert tabular data.
        assertTablePresent("user");
        assertTableRowsEqual("user", 1, new String[][]{
                new String[]{"login", login},   // login row
                new String[]{"name", login}     // name row
        });

        assertLinkPresent("user.edit");
        clickLink("user.edit");

        EditUserForm form = new EditUserForm(tester);

        form.assertFormElements(login);
        form.cancelFormElements("S. O. MeBody");

        // assert tabular data.
        assertTablePresent("user");
        assertTableRowsEqual("user", 1, new String[][]{
                new String[]{"login", login},   // login row
                new String[]{"name", login}     // name row
        });
    }

    public void testEditUserValidation()
    {
        clickLink("user.edit");

        EditUserForm form = new EditUserForm(tester);

        form.assertFormElements(login);
        form.saveFormElements("");

        // assert validation failed.
        form.assertFormElements("");
        assertTextPresent("required");
    }

    public void testEditPassword()
    {
        assertLinkPresent("user.edit");
        clickLink("user.edit");

        EditPasswordForm form = new EditPasswordForm(tester);
        form.assertFormElements("", "", "");
        form.saveFormElements(login, "newPassword", "newPassword");
        form.assertFormNotPresent();

        // assert that we are back on the preferences page.
        assertTablePresent("user");
        assertLinkPresent("user.edit");

        // now to verify that the password was actually changed.
        login(login, "newPassword");
        assertTextPresent("welcome");
    }

    public void testEditPasswordValidation()
    {
        assertLinkPresent("user.edit");
        clickLink("user.edit");

        EditPasswordForm form = new EditPasswordForm(tester);
        form.assertFormElements("", "", "");

        // check that each field is required.
        form.saveFormElements("a", "a", "");
        assertTextPresent("required");

        form.saveFormElements("b", "", "b");
        assertTextPresent("required");

        form.saveFormElements("", "c", "c");
        assertTextPresent("required");

        // check that the current password is correctly checked.
        form.saveFormElements("incorrect", "a", "a");
        assertTextPresent("does not match");

        // check that the new password and confirm password are correctly checked.
        form.saveFormElements(login, "a", "b");
        assertTextPresent("does not match");
    }

    public void testAddAlias()
    {
        assertAndClick("alias.add");

        AddAliasForm form = new AddAliasForm(tester);
        form.assertFormPresent();

        form.saveFormElements("alias1");

        assertAliasesTable("alias1");
    }

    public void testAddAliasValidation()
    {
        testAddAlias();

        assertAndClick("alias.add");

        AddAliasForm form = new AddAliasForm(tester);
        form.assertFormPresent();

        form.saveFormElements("");
        form.assertFormPresent();
        assertTextPresent("alias is required");

        form.saveFormElements("alias1");
        form.assertFormPresent();
        assertTextPresent("you have already configured an alias with the same value");
    }

    public void testAddAliasCancel()
    {
        assertAndClick("alias.add");

        AddAliasForm form = new AddAliasForm(tester);
        form.assertFormPresent();
        form.cancelFormElements("aliasCancelled");
        assertAliasesTable();
    }

    public void testDeleteAlias()
    {
        testAddAlias();
        assertAndClick("deleteAlias1");
        assertAliasesTable();
    }

    public void testEditSettings()
    {
        assertAndClick("user.settings");

        UserSettingsForm form = new UserSettingsForm(tester);
        form.assertFormPresent();
        form.assertFormElements("welcome", "true", "60", "60");
        form.saveFormElements("dashboard", "false", "60", "40");

        assertSettingsTable("dashboard", "never", "every 40 seconds");

        assertAndClick("user.settings");
        form.assertFormPresent();
        form.assertFormElements("dashboard", "false", "60", "40");
    }

    public void testEditSettingsCancel()
    {
        assertAndClick("user.settings");

        UserSettingsForm form = new UserSettingsForm(tester);
        form.assertFormPresent();
        form.assertFormElements("welcome", "true", "60", "60");
        form.cancelFormElements("dashboard", "false", null, "30");

        assertSettingsTable("welcome", "every 60 seconds", "every 60 seconds");
    }

    public void testEditSettingsValidation()
    {
        assertAndClick("user.settings");

        UserSettingsForm form = new UserSettingsForm(tester);
        form.assertFormPresent();

        form.assertFormElements("welcome", "true", "60", "60");
        form.saveFormElements("dashboard", "true", "0", "10");
        form.assertFormPresent();
        assertTextPresent("refresh interval must be a positive number");
        form.saveFormElements("dashboard", "true", "10", "0");
        form.assertFormPresent();
        assertTextPresent("interval must be positive");
    }

    private void assertAliasesTable(String ...aliases)
    {
        assertTablePresent("aliases");
        ExpectedTable expectedTable = new ExpectedTable();
        expectedTable.appendRow(new ExpectedRow(new String[]{"aliases", "aliases"}));
        expectedTable.appendRow(new ExpectedRow(new String[]{"alias", "actions"}));
        for (String alias : aliases)
        {
            expectedTable.appendRow(new ExpectedRow(new String[]{alias, "delete"}));
        }
        expectedTable.appendRow(new ExpectedRow(new String[]{"add new alias", "add new alias"}));

        assertTableEquals("aliases", expectedTable);
    }

    private void assertSettingsTable(String defaultAction, String refreshInterval, String tailInterval)
    {
        assertTablePresent("settings");
        assertTableRowsEqual("settings", 1, new String[][]{
                new String[]{"default page", defaultAction},
                new String[]{"refresh live content", refreshInterval},
                new String[]{"refresh recipe log tail", tailInterval}
        });
    }

    public void testSubscriptionFormValidation()
    {
        // can not create subscription without project
        // can not create subscription without contacts.
        assertLinkNotPresent("subscription.create");

        assertTablePresent("subscriptions");
        assertTableRowsEqual("subscriptions", 1, new String[][]{
                new String[]{"contact", "projects", "condition", "actions"},
                new String[]{CONTACT_REQUIRED, CONTACT_REQUIRED, CONTACT_REQUIRED, CONTACT_REQUIRED}
        });
    }

    private void createEmailContactPoint(String name, String email)
    {
        EmailContactForm emailForm = emailSetup();
        emailForm.saveFormElements(name, email, "html");
        emailForm.assertFormNotPresent();
        assertTextPresent(name);
        assertTextPresent(email);
    }

    public void testEditContactPoint()
    {
        // create a contact point.
        assertLinkPresentWithText(CREATE_CONTACT_LINK);
        createEmailContactPoint("home", "user@example.com");

        // edit the contact point.
        assertLinkPresent("edit_home");
        clickLink("edit_home");

        EmailContactForm form = new EmailContactForm(tester, false);
        form.assertFormElements("home", "user@example.com", "html");
        form.saveFormElements("newHome", "anotherUser@example.com", "plain");

        // ensure that we have correctly changed the email contact.
        assertLinkPresent("edit_newHome");
        assertLinkNotPresent("edit_home");

        clickLink("edit_newHome");
        form.assertFormPresent();
        form.assertFormElements("newHome", "anotherUser@example.com", "plain");
        form.cancelFormElements("cancelled", "nouser@example.com", "html");

        // assert that the contact appears as expected.
        assertTextPresent("newHome");
        assertTextPresent("anotherUser@example.com");
        assertContactsTable("newHome", "anotherUser@example.com");
    }

    private JabberContactForm jabberSetup()
    {
        assertAndClick("contact.create");

        CreateContactForm contactForm = new CreateContactForm(tester);
        contactForm.assertFormPresent();
        assertOptionsEqual("contact", new String[]{ "email", "jabber" });
        contactForm.nextFormElements("jabber");

        JabberContactForm jabberForm = new JabberContactForm(tester, true);
        jabberForm.assertFormPresent();
        return jabberForm;
    }

    public void testAddJabberContactPoint()
    {
        addJabber(JABBER_CONTACT);
        assertContactsTable(JABBER_CONTACT, "jabbername");
    }

    private void addJabber(String name)
    {
        JabberContactForm jabberForm = jabberSetup();
        jabberForm.saveFormElements(name, "jabbername");
    }

    public void testAddJabberContactValidation()
    {
        JabberContactForm jabberForm = jabberSetup();
        jabberForm.saveFormElements("", "");
        jabberForm.assertFormPresent();
        assertTextPresent("name is required");
        assertTextPresent("username is required");
    }

    public void testAddJabberContactCancel()
    {
        JabberContactForm jabberForm = jabberSetup();
        jabberForm.cancelFormElements("hello", "sailor");
        assertTablePresent("contacts");
        assertTextNotPresent("sailor");
    }

    public void testEditJabberContact()
    {
        testAddJabberContactPoint();
        assertAndClick("edit_myjabber");
        JabberContactForm form = new JabberContactForm(tester, false);
        form.assertFormPresent();
        form.saveFormElements("newjabber", "newuid");
        assertContactsTable("newjabber", "newuid");
    }

    public void testEditJabberContactValidation()
    {
        testAddJabberContactPoint();
        assertAndClick("edit_myjabber");
        JabberContactForm form = new JabberContactForm(tester, false);
        form.assertFormPresent();
        form.saveFormElements("", "");
        form.assertFormPresent();
        assertTextPresent("name is required");
        assertTextPresent("username is required");
    }

    public void testEditJabberContactCancel()
    {
        testAddJabberContactPoint();
        assertAndClick("edit_myjabber");
        JabberContactForm form = new JabberContactForm(tester, false);
        form.assertFormPresent();
        form.cancelFormElements("newjabber", "newuid");
        assertContactsTable(JABBER_CONTACT, "jabbername");
    }

    public void testDeleteJabberContact()
    {
        testAddJabberContactPoint();
        assertAndClick("delete_myjabber");
        assertTablePresent("contacts");
        assertTextNotPresent(JABBER_CONTACT);
    }

    public void testCreateSubscription()
    {
        addJabber("jabbier");

        clickLink("subscription.create");
        SubscriptionForm form = new SubscriptionForm(tester, true);
        form.assertFormPresent();
        String projectId = tester.getDialog().getOptionValuesFor("projects")[0];
        String projectName = tester.getDialog().getOptionsFor("projects")[0];
        String contactId = tester.getDialog().getOptionValuesFor("contactPointId")[0];
        String contactName = tester.getDialog().getOptionsFor("contactPointId")[0];
        form.saveFormElements(contactId, projectId, "changed");

        assertSubscriptionsTable(projectName, contactName, "changed");
    }

    public void testCreateSubscriptionAllProjects()
    {
        addJabber("jabbier");

        clickLink("subscription.create");
        SubscriptionForm form = new SubscriptionForm(tester, true);
        form.assertFormPresent();
        String contactId = tester.getDialog().getOptionValuesFor("contactPointId")[0];
        String contactName = tester.getDialog().getOptionsFor("contactPointId")[0];
        form.saveFormElements(contactId, "", "changed");

        assertSubscriptionsTable("[all]", contactName, "changed");
    }

    public void testEditSubscription()
    {
        addJabber("zlast");
        testCreateSubscription();

        clickLinkWithText("edit", 4);
        SubscriptionForm form = new SubscriptionForm(tester, false);
        form.assertFormPresent();

        String projectId = tester.getDialog().getOptionValuesFor("projects")[1];
        String projectName = tester.getDialog().getOptionsFor("projects")[1];
        String contactId = tester.getDialog().getOptionValuesFor("contactPointId")[1];
        String contactName = tester.getDialog().getOptionsFor("contactPointId")[1];
        form.saveFormElements(contactId, projectId, "true");

        assertSubscriptionsTable(projectName, contactName, "true");
    }

    private void assertContactsTable(String name, String uid)
    {
        ExpectedTable expectedTable = new ExpectedTable();
        expectedTable.appendRow(new ExpectedRow(new String[]{"name", "uid", "actions", "actions"}));
        expectedTable.appendRow(new ExpectedRow(new String[]{name, uid, "edit", "delete"}));
        assertTableRowsEqual("contacts", 1, expectedTable);
    }

    private void assertSubscriptionsTable(String project, String contact, String condition)
    {
        ExpectedTable expectedTable = new ExpectedTable();
        expectedTable.appendRow(new ExpectedRow(new String[]{"contact", "projects", "condition", "actions", "actions"}));
        expectedTable.appendRow(new ExpectedRow(new String[]{contact, project, condition, "edit", "delete"}));
        assertTableRowsEqual("subscriptions", 1, expectedTable);
    }

    private void navigateToPreferences()
    {
        gotoPage("/");
        clickLinkWithText("dashboard");
        clickLinkWithText("preferences");
    }
}
