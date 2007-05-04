package com.zutubi.pulse.events.build;

import com.zutubi.pulse.core.BuildRevision;
import com.zutubi.pulse.core.model.Entity;
import com.zutubi.pulse.model.*;
import com.zutubi.pulse.personal.PatchArchive;
import com.zutubi.pulse.prototype.config.ProjectConfiguration;

/**
 */
public class PersonalBuildRequestEvent extends AbstractBuildRequestEvent
{
    private long number;
    private User user;
    private PatchArchive patch;

    public PersonalBuildRequestEvent(Object source, long number, BuildRevision revision, User user, PatchArchive patch, ProjectConfiguration projectConfig, Project project)
    {
        super(source, revision, projectConfig, project);
        this.number = number;
        this.user = user;
        this.patch = patch;
    }

    public Entity getOwner()
    {
        return user;
    }

    public boolean isPersonal()
    {
        return true;
    }

    public BuildResult createResult(ProjectManager projectManager, UserManager userManager)
    {
        return new BuildResult(user, getProject(), number);
    }

    public PatchArchive getPatch()
    {
        return patch;
    }

    public long getNumber()
    {
        return number;
    }

    public User getUser()
    {
        return user;
    }

    public String toString()
    {
        String result = "Personal Build Request Event: " + number;
        if(user != null)
        {
            result += ": " + user.getLogin();
        }
        return result;
    }
}
