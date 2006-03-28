package com.cinnamonbob.model;

import com.cinnamonbob.core.model.Entity;

import java.util.LinkedList;
import java.util.List;

/**
 * 
 *
 */
public class Project extends Entity
{
    public enum State
    {
        /**
         * There is a build running for this project.
         */
        BUILDING,
        /**
         * No builds running for the project at the moment.
         */
        IDLE,
        /**
         * Currently paused: triggers will be ignored while in this state.
         */
        PAUSED,
        /**
         * Project is building, but will be paused when the current build
         * is completed.
         */
        PAUSING
    }

    public static final int DEFAULT_WORK_DIR_BUILDS = 10;

    private String name;
    private String description;
    private String url;
    private BobFileDetails bobFileDetails;
    private List<CleanupRule> cleanupRules = new LinkedList<CleanupRule>();
    private Scm scm;
    private State state = State.IDLE;


    private List<BuildSpecification> buildSpecifications;

    public Project()
    {
    }

    public Project(String name, String description)
    {
        this(name, description, new CustomBobFileDetails("bob.xml"));
    }

    public Project(String name, String description, BobFileDetails bobFileDetails)
    {
        this.name = name;
        this.description = description;
        this.bobFileDetails = bobFileDetails;
        this.addCleanupRule(new CleanupRule(true, null, DEFAULT_WORK_DIR_BUILDS, CleanupRule.CleanupUnit.BUILDS));
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

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public Scm getScm()
    {
        return scm;
    }

    public void setScm(Scm scm)
    {
        this.scm = scm;
    }

    public List<BuildSpecification> getBuildSpecifications()
    {
        if (buildSpecifications == null)
        {
            buildSpecifications = new LinkedList<BuildSpecification>();
        }

        return buildSpecifications;
    }

    public void addBuildSpecification(BuildSpecification specification)
    {
        getBuildSpecifications().add(specification);
    }

    private void setBuildSpecifications(List<BuildSpecification> buildSpecifications)
    {
        this.buildSpecifications = buildSpecifications;
    }

    public BuildSpecification getBuildSpecification(String name)
    {
        for (BuildSpecification spec : buildSpecifications)
        {
            if (spec.getName().compareToIgnoreCase(name) == 0)
            {
                return spec;
            }
        }
        return null;
    }

    public boolean remove(BuildSpecification buildSpecification)
    {
        return buildSpecifications.remove(buildSpecification);
    }

    public BobFileDetails getBobFileDetails()
    {
        return bobFileDetails;
    }

    public void setBobFileDetails(BobFileDetails bobFileDetails)
    {
        this.bobFileDetails = bobFileDetails;
    }

    public List<CleanupRule> getCleanupRules()
    {
        return cleanupRules;
    }

    private void setCleanupRules(List<CleanupRule> cleanupRules)
    {
        this.cleanupRules = cleanupRules;
    }

    public void addCleanupRule(CleanupRule rule)
    {
        cleanupRules.add(rule);
    }

    public CleanupRule getCleanupRule(long id)
    {
        for (CleanupRule rule : cleanupRules)
        {
            if (rule.getId() == id)
            {
                return rule;
            }
        }

        return null;
    }

    public void removeCleanupRule(long id)
    {
        CleanupRule deadRuleWalking = getCleanupRule(id);
        if (deadRuleWalking != null)
        {
            cleanupRules.remove(deadRuleWalking);
        }
    }

    public State getState()
    {
        return state;
    }

    private String getStateName()
    {
        return state.toString();
    }

    private void setStateName(String stateName)
    {
        state = State.valueOf(stateName);
    }

    public synchronized boolean isPaused()
    {
        return state == State.PAUSED || state == State.PAUSING;
    }

    public synchronized void buildCommenced()
    {
        state = State.BUILDING;
    }

    public synchronized void buildCompleted()
    {
        if (state == State.PAUSING)
        {
            state = State.PAUSED;
        }
        else
        {
            state = State.IDLE;
        }
    }

    public synchronized void pause()
    {
        switch (state)
        {
            case BUILDING:
                state = State.PAUSING;
                break;
            case IDLE:
                state = State.PAUSED;
                break;
        }
    }

    public synchronized void resume()
    {
        switch (state)
        {
            case PAUSED:
                state = State.IDLE;
                break;
            case PAUSING:
                state = State.BUILDING;
                break;
        }

    }
}
