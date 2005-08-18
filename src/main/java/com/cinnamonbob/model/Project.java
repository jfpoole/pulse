package com.cinnamonbob.model;

import java.util.List;
import java.util.LinkedList;

/**
 * 
 *
 */
public class Project extends Entity
{
    private String name;
    private String description;
    private String bobFile;
    private List<Scm> scms;

    public Project()
    {
        bobFile = "bob.xml";
    }

    public Project(String name, String description)
    {
        this.name = name;
        this.description = description;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<Scm> getScms()
    {
        if(scms == null)
        {
            scms = new LinkedList<Scm>();
        }
        
        return scms;
    }

    public void addScm(Scm scm)
    {
        getScms().add(scm);
        scm.setProject(this);
    }

    private void setScms(List<Scm> scms)
    {
        this.scms = scms;
    }

    public Scm getScm(String name)
    {
        for (Scm scm : scms)
        {
            if (scm.getName().compareToIgnoreCase(name) == 0)
            {
                return scm;
            }
        }
        return null;
    }

    public boolean remove(Scm scm)
    {
        if (scms.remove(scm))
        {
            scm.setProject(null);
            return true;
        }
        return false;
    }

    public String getBobFile()
    {
        return bobFile;
    }

    public void setBobFile(String bobFile)
    {
        this.bobFile = bobFile;
    }
}
