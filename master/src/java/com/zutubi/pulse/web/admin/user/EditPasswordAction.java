/********************************************************************************
 @COPYRIGHT@
 ********************************************************************************/
package com.zutubi.pulse.web.admin.user;

import com.zutubi.pulse.util.RandomUtils;
import com.zutubi.pulse.web.user.UserActionSupport;
import com.zutubi.pulse.model.User;

/**
 * <class-comment/>
 */
public class EditPasswordAction extends UserActionSupport
{
    /**
     * The new password.
     */
    private String password;

    /**
     * The new password confirmation.
     */
    private String confirm;

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getConfirm()
    {
        return confirm;
    }

    public void setConfirm(String confirm)
    {
        this.confirm = confirm;
    }

    public void validate()
    {
        if (hasErrors())
        {
            return;
        }
        if (getUser() == null)
        {
            addUnknownUserFieldError();
            return;
        }

        if (!password.equals(confirm))
        {
            addFieldError("confirm", getText("password.confirm.mismatch"));
        }
    }

    /**
     * Change the specified users password to some randomly generated password.
     *
     */
    public String doReset()
    {
        //TODO: There needs to be a way to send out some form of notification of this password.
        String rawPassword = RandomUtils.randomString(8);
        User user = getUser();
        getUserManager().setPassword(user, rawPassword);
        getUserManager().save(user);
        return SUCCESS;
    }

    /**
     * Edit the specified users password, setting it to the specified password.
     *
     */
    public String execute()
    {
        User user = getUser();
        getUserManager().setPassword(user, password);
        getUserManager().save(user);
        return SUCCESS;
    }
}
