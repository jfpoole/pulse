package com.zutubi.pulse.web.project;

import com.zutubi.pulse.model.Project;
import com.zutubi.pulse.model.User;
import com.zutubi.pulse.model.UserManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 */
public class AddProjectAdminsAction extends ProjectActionSupport
{
    private Project project;
    private List<String> admins;
    private UserManager userManager;

    public Project getProject()
    {
        return project;
    }

    public Map<String, String> getUsers()
    {
        Map<String, String> users = new HashMap<String, String>();
        for(User user: userManager.getAllUsers())
        {
            if(!project.hasAdmin(user.getLogin()))
            {
                users.put(user.getLogin(), user.getName());
            }
        }

        return users;
    }

    public List<String> getAdmins()
    {
        if(admins == null)
        {
            admins = new LinkedList<String>();
        }
        return admins;
    }

    public void setAdmins(List<String> admins)
    {
        this.admins = admins;
    }

    public String doInput()
    {
        project = getProjectManager().getProject(projectId);
        return INPUT;
    }

    public void validate()
    {
        project = getProjectManager().getProject(projectId);
        if (project == null)
        {
            addActionError("Unknown project [" + projectId + "]");
        }
    }

    public String execute()
    {
        for(String login: getAdmins())
        {
            project.addAdmin(login);
        }
        getProjectManager().save(project);
        return SUCCESS;
    }

    public void setUserManager(UserManager userManager)
    {
        this.userManager = userManager;
    }
}
