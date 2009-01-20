package com.zutubi.pulse.master.tove.config.project;

import com.zutubi.pulse.master.model.BuildManager;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.pulse.master.model.ProjectManager;
import com.zutubi.pulse.master.tove.config.DatabaseStateCleanupTaskSupport;
import com.zutubi.tove.config.ToveRuntimeException;

/**
 * Cleans up the state associated with a deleted project.
 */
class ProjectStateCleanupTask extends DatabaseStateCleanupTaskSupport
{
    private ProjectConfiguration instance;
    private ProjectManager projectManager;

    public ProjectStateCleanupTask(ProjectConfiguration instance, ProjectManager projectManager, BuildManager buildManager)
    {
        super(instance.getConfigurationPath(), buildManager);
        this.projectManager = projectManager;
        this.instance = instance;
    }

    public void cleanupState()
    {
        long projectId = instance.getProjectId();

        projectManager.lockProjectState(projectId);
        try
        {
            Project project = projectManager.getProject(projectId, true);
            if (project != null)
            {
                if (project.isTransitionValid(Project.Transition.DELETE))
                {
                    projectManager.delete(project);
                }
                else
                {
                    Project.State state = project.getState();
                    if (state.isBuilding())
                    {
                        throw new ToveRuntimeException("Unable to delete project as a build is running.  The project may be deleted when it becomes idle (consider pausing the project).");
                    }
                    else
                    {
                        throw new ToveRuntimeException("Unable to delete project while in state '" + state + "'");
                    }
                }
            }
        }
        finally
        {
            projectManager.unlockProjectState(projectId);
        }
    }
}