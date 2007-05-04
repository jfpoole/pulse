package com.zutubi.pulse.web.project;

import com.opensymphony.util.TextUtils;
import com.zutubi.pulse.model.ResourceRequirement;

/**
 */
public class BuildStageResourceActionSupport extends BuildStageActionSupport
{
    protected ResourceRequirement resourceRequirement = new ResourceRequirement();

    public ResourceRequirement getResourceRequirement()
    {
        return resourceRequirement;
    }

    protected void lookupNode()
    {
    }

    protected void addFieldsToResource()
    {
        if(!TextUtils.stringSet(resourceRequirement.getVersion()))
        {
            resourceRequirement.setVersion(null);
        }
    }
}
