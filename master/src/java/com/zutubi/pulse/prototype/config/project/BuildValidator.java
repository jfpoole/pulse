package com.zutubi.pulse.prototype.config.project;

import com.zutubi.prototype.config.ConfigurationValidationContext;
import com.zutubi.prototype.type.record.PathUtils;
import com.zutubi.pulse.model.BuildManager;
import com.zutubi.pulse.model.BuildResult;
import com.zutubi.pulse.model.Project;
import com.zutubi.pulse.model.ProjectManager;
import com.zutubi.validation.validators.StringFieldValidatorSupport;

/**
 */
public class BuildValidator extends StringFieldValidatorSupport
{
    private ProjectManager projectManager;
    private BuildManager buildManager;

    public void validateStringField(String build)
    {
        ConfigurationValidationContext context = (ConfigurationValidationContext) getValidationContext();
        String[] parentPathElenents = PathUtils.getPathElements(context.getParentPath());
        if(parentPathElenents.length < 2)
        {
            context.addFieldError(getFieldName(), context.getText("invalid.parent.path", context.getParentPath()));
            return;
        }

        String projectName = parentPathElenents[1];
        Project project = projectManager.getProject(projectName, true);
        if (project == null)
        {
            context.addFieldError(getFieldName(), context.getText("invalid.project", projectName));
            return;
        }

        BuildResult buildResult = buildManager.getByProjectAndVirtualId(project, build);
        if(buildResult == null || !buildResult.completed())
        {
            context.addFieldError(getFieldName(), context.getText("invalid.build", build));
        }
    }

    public void setProjectManager(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }
}
