/* Copyright 2017 Zutubi Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zutubi.pulse.master.xwork.actions;

import com.zutubi.pulse.master.model.User;
import com.zutubi.pulse.master.security.SecurityUtils;
import com.zutubi.pulse.master.tove.config.admin.GlobalConfiguration;
import com.zutubi.tove.security.AccessManager;

import java.util.Arrays;

import static com.zutubi.pulse.master.tove.config.MasterConfigurationRegistry.*;

/**
 * The default action controls the page presented to the user when they first arrive at the
 * application.
 *
 * It handles a number of things.
 * 1) if the system has not been setup, then it will direct the user to the setup wizard.
 * 2) directs the user to their default page, either the welcome page or the dashboard.
 */
public class DefaultAction extends ActionSupport
{
    /**
     * The welcome result. See the xwork config for details.
     */
    public static final String WELCOME_ACTION = "welcome";

    /**
     * The dashboard result. See the xwork config for details.
     */
    public static final String DASHBOARD_ACTION = "dashboard";

    /**
     * The browse view result. See xwork config for details.
     */
    public static final String BROWSE_ACTION = "browse";

    /**
     * The setup result. See the xwork config for details.
     */
    private static final String SETUP_ADMIN = "setupAdmin";

    private boolean configureAllowed = false;

    public boolean isConfigureAllowed()
    {
        return configureAllowed;
    }

    public String execute()
    {
        if (userManager.getUserCount() == 0)
        {
            return SETUP_ADMIN;
        }

        String login = SecurityUtils.getLoggedInUsername();
        if(login == null)
        {
            return BROWSE_ACTION;
        }
        else
        {
            User user = userManager.getUser(login);
            if (user == null)
            {
                return BROWSE_ACTION;
            }
            else
            {
                for (String scope: Arrays.asList(AGENTS_SCOPE, PROJECTS_SCOPE, USERS_SCOPE, GlobalConfiguration.SCOPE_NAME))
                {
                    if (accessManager.hasPermission(AccessManager.ACTION_CREATE, scope))
                    {
                        configureAllowed = true;
                        break;
                    }
                }

                return user.getPreferences().getDefaultAction();
            }
        }
    }
}
