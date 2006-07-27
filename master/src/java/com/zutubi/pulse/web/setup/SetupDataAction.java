package com.zutubi.pulse.web.setup;

import com.opensymphony.util.TextUtils;
import com.zutubi.pulse.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.bootstrap.SetupManager;

import java.io.File;

/**
 * <class-comment/>
 */
public class SetupDataAction extends SetupActionSupport
{
    private MasterConfigurationManager configurationManager;
    private SetupManager setupManager;

    private String data;

    public String getData()
    {
        return data;
    }

    public void setData(String data)
    {
        this.data = data;
    }

    public void validate()
    {
        // attempt to create the data directory. If this fails, we need to ask the
        // user for another directory.
        File data = new File(this.data);
        if (!data.exists() && !data.mkdirs())
        {
            addFieldError("data", getText("Failed to create the specified data directory."));
        }
    }

    public String doInput() throws Exception
    {
        // set the default.
        String userHome = System.getProperty("user.home");
        if (TextUtils.stringSet(userHome))
        {
            this.data = userHome + File.separatorChar + ".pulse" + File.separatorChar + "data";
        }
        else
        {
            this.data = "data";
        }

        // make the path the shortest possible.
        this.data = new File(this.data).getCanonicalPath();

        return INPUT;
    }

    public String execute() throws Exception
    {
        File home = new File(this.data);
        configurationManager.setPulseData(home);
        setupManager.requestDataComplete();
        
        return SUCCESS;
    }

    /**
     * Required resource.
     *
     * @param configurationManager
     */
    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    /**
     * Required resource.
     *
     * @param setupManager
     */
    public void setSetupManager(SetupManager setupManager)
    {
        this.setupManager = setupManager;
    }
}
