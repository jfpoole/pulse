package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.util.RandomUtils;
import com.zutubi.pulse.acceptance.forms.admin.EditPasswordForm;

/**
 * <class-comment/>
 */
public class UserAdministrationAcceptanceTest extends BaseAcceptanceTest
{

    public UserAdministrationAcceptanceTest()
    {
    }

    public UserAdministrationAcceptanceTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        login("admin", "admin");

        // navigate to user admin tab.
        navigateToUserAdministration();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testCreateStandardUser()
    {
        // create random login name.
        String login = RandomUtils.randomString(10);

        assertUserNotExists(login);

        submitCreateUserForm(login, login, login, login, false);

        // assert user does exist.
        assertUserExists(login);

        // assert form is reset.
        assertFormReset();

        // check that we can login with this new user.
        clickLinkWithText("logout");

        login(login, login);

        // if login was successful, should see the welcome page and a logout link.
        assertTextPresent(":: welcome ::");
        assertLinkPresentWithText("logout");

        // ensure that this user does not have admin permissions.
        assertLinkNotPresentWithText("administration");
    }

    public void testCreateAdminUser()
    {
        // create random login name.
        String login = RandomUtils.randomString(10);

        assertUserNotExists(login);

        submitCreateUserForm(login, login, login, login, true);

        // assert user does exist.
        assertUserExists(login);

        // assert form is reset.
        assertFormReset();

        // check that we can login with this new user.
        clickLinkWithText("logout");

        login(login, login);

        // if login was successful, should see the welcome page and a logout link.
        assertTextPresent(":: welcome ::");
        assertLinkPresentWithText("logout");

        // ensure that this user does not have admin permissions.
        assertLinkPresentWithText("administration");

    }

    public void testCreateUserValidation()
    {
        // create random login name.
        String login = RandomUtils.randomString(10);

        // check validation - login is required.
        submitCreateUserForm("", login, login, login, false);

        // should get an error message.
        assertTextPresent("required");
        assertLinkNotPresentWithText(login);
        assertFormElementEmpty(USER_CREATE_LOGIN);
        assertFormElementEquals(USER_CREATE_NAME, login);

        // check validation - password is required.
        submitCreateUserForm(login, login, "", "", false);

        assertTextPresent("required");
        assertLinkNotPresentWithText(login);
        assertFormElementEquals(USER_CREATE_LOGIN, login);
        assertFormElementEquals(USER_CREATE_NAME, login);

        // check validation - password and confirmation mismatch
        submitCreateUserForm(login, login, login, "something not very random", false);

        assertTextPresent("does not match");
        assertLinkNotPresentWithText(login);
        assertFormElementEquals(USER_CREATE_LOGIN, login);
        assertFormElementEquals(USER_CREATE_NAME, login);
    }

    public void testDeleteUser()
    {
        // create a user to delete - assume that user creation is successful?
        String login = RandomUtils.randomString(10);
        submitCreateUserForm(login, login, login, login, false);
        // check that it worked.
        assertLinkPresentWithText(login);
        assertLinkPresent("delete_" + login);

        clickLink("delete_" + login);

        // hmm, now there should be a confirm delete dialog box that appears,
        // but for some reason, i does not appear to exist in the list of open windows.
        // odd...

        assertLinkNotPresentWithText(login);
    }

    public void testCanNotDeleteSelf()
    {
        // create a user to delete - assume that user creation is successful?
        String login = RandomUtils.randomString(10);
        submitCreateUserForm(login, login, login, login, true);

        // login.
        login(login, login);
        navigateToUserAdministration();

        assertLinkPresent("delete_" + login);

        clickLink("delete_" + login);

        // assert that we are still there.
        assertLinkPresent("delete_" + login);
        assertTextPresent("can not delete");
    }

    public void testViewUser()
    {
        // create user.
        String login = RandomUtils.randomString(10);
        submitCreateUserForm(login, login, login, login, false);

        // view user
        assertLinkPresentWithText(login);
        clickLinkWithText(login);

        // assert tabular data.
        assertTablePresent("user");
        assertTableRowsEqual("user", 1, new String[][]{
                new String[]{"login", login},   // login row
                new String[]{"name", login}     // name row
        });

        // switch to user, create contacts and subscriptions, assert they appear.

        assertTablePresent("contacts");

        assertTablePresent("subscriptions");
    }

    public void testChangeUserPassword()
    {
        // create a user.
        String login = RandomUtils.randomString(7);
        submitCreateUserForm(login, login, login, login, false);

        assertLinkPresent("edit_" + login);
        clickLink("edit_" + login);

        EditPasswordForm form = new EditPasswordForm(tester);
        form.assertFormElements("", "");
        form.saveFormElements("newPassword", "newPassword");

        // assert that we are back on the manage users

        // now to verify that the password was actually changed.
        login(login, "newPassword");
        assertTextPresent("welcome");
    }

    public void testChangeUserPasswordValidation()
    {
        // create a user.
        String login = RandomUtils.randomString(7);
        submitCreateUserForm(login, login, login, login, false);

        assertLinkPresent("edit_" + login);
        clickLink("edit_" + login);

        EditPasswordForm form = new EditPasswordForm(tester);
        form.assertFormElements("", "");

        // check that each field is required.
        form.saveFormElements("a", "");
        assertTextPresent("required");

        form.saveFormElements("", "b");
        assertTextPresent("required");

        // check that the new password and confirm password are correctly checked.
        form.saveFormElements("a", "b");
        assertTextPresent("does not match");
    }

    private void assertFormReset()
    {
        assertFormElementEmpty(USER_CREATE_LOGIN);
        assertFormElementEmpty(USER_CREATE_NAME);
        assertFormElementEmpty(USER_CREATE_PASSWORD);
        assertFormElementEmpty(USER_CREATE_CONFIRM);
        assertCheckboxNotSelected(USER_CREATE_ADMIN);
    }

    private void assertUserExists(String login)
    {
        // PROBLEM: this will only work while there is no pagination. we need another way to
        // lookup a user, a more direct method.
        assertTextPresent(login);
        assertLinkPresentWithText(login);
    }

    private void assertUserNotExists(String login)
    {
        // PROBLEM: this will only work while there is no pagination. we need another way to
        // lookup a user, a more direct method.
        assertTextNotPresent(login);
        assertLinkNotPresentWithText(login);
    }
}
