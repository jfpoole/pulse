package com.zutubi.pulse.master.events.build;

import com.zutubi.events.Event;
import com.zutubi.pulse.core.BuildRevision;
import com.zutubi.pulse.core.config.ResourcePropertyConfiguration;
import com.zutubi.pulse.core.model.NamedEntity;
import com.zutubi.pulse.master.model.*;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.util.TimeStamps;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Base class for build requests.  Specific subclasses are used to
 * differentiate project and personal build requests.
 */
public abstract class BuildRequestEvent extends Event implements Comparable
{
    private BuildRevision revision;
    private long queued;
    protected ProjectConfiguration projectConfig;
    protected TriggerOptions options;

    /**
     * If set, this meta build id indicates that this build request is
     * associated with an existing build.
     */
    private long metaBuildId;

    /**
     * A list of project builds that the build generated by this request
     * depends on.  This is used for reporting purposes and assumes that
     * the referenced project builds have a metabuildid that matches this
     * request.
     */
    protected List<Project> dependentProjects = new LinkedList<Project>();

    /**
     * @param source        the event source
     * @param revision      build revision to use for the build, may not be
     *                      initialised if the revision should float
     * @param projectConfig configuration of the project to build, snapshotted
     *                      in time for this entire build
     * @param options       the options for this build.
     */
    public BuildRequestEvent(Object source, BuildRevision revision, ProjectConfiguration projectConfig, TriggerOptions options)
    {
        super(source);
        this.revision = revision;
        this.projectConfig = projectConfig;
        this.options = options;
        this.queued = System.currentTimeMillis();
    }

    public abstract NamedEntity getOwner();
    public abstract boolean isPersonal();

    /**
     * Create a new persistent instance of the build result represented by this request.
     *
     * @param projectManager    project manager resource
     * @param buildManager      build manager resource
     *
     * @return a persistent build result instance.
     */
    public abstract BuildResult createResult(ProjectManager projectManager, BuildManager buildManager);

    /**
     * The status for the requested build.
     * 
     * @return  the requested builds status string.
     */
    public abstract String getStatus();

    /**
     * Get the version of the build for this request.  The version is taken from
     * the projects dependencies configuration unless overriden by the options.
     * <p/>
     * Note that this is an unresolved value of the version, and will need to be
     * resolved if required.
     *
     * @return the version to be used by this build associated with this request.
     */
    public String getVersion()
    {
        String version = projectConfig.getDependencies().getVersion();
        if (options.hasVersion())
        {
            version = options.getVersion();
        }
        return version;
    }

    public BuildRevision getRevision()
    {
        return revision;
    }

    public void setRevision(BuildRevision revision)
    {
        this.revision = revision;
    }

    /**
     * Get the id of the project associated with this build request.
     *
     * @return the projects id.
     */
    public long getProjectId()
    {
        return getProjectConfig().getProjectId();
    }

    public ProjectConfiguration getProjectConfig()
    {
        return projectConfig;
    }

    public long getQueued()
    {
        return queued;
    }

    public String getPrettyQueueTime()
    {
        return TimeStamps.getPrettyTime(queued);
    }

    public TriggerOptions getOptions()
    {
        return options;
    }

    public BuildReason getReason()
    {
        return options.getReason();
    }

    public Collection<ResourcePropertyConfiguration> getProperties()
    {
        return options.getProperties();
    }

    public String getRequestSource()
    {
        return options.getSource();
    }

    public boolean isReplaceable()
    {
        return options.isReplaceable();
    }

    public long getMetaBuildId()
    {
        return metaBuildId;
    }

    public void setMetaBuildId(long buildId)
    {
        this.metaBuildId = buildId;
    }

    public int compareTo(Object o)
    {
        BuildRequestEvent other = (BuildRequestEvent) o;
        if (queued > other.queued)
        {
            return 1;
        }
        else if (queued < other.queued)
        {
            return -1;
        }
        else
        {
            return 0;
        }
    }

    /**
     * WARNING: This method should only be used by the scheduling system
     * and is used to record dependent projects for this specific build.
     */
    public void addDependentOwner(Project project)
    {
        this.dependentProjects.add(project);
    }

    /**
     * Returns true if this request is allowed to jump the queue, false
     * otherwise.
     *
     * @return true if this request is allowed to jump the queue.
     *
     * @see com.zutubi.pulse.master.model.TriggerOptions#isJumpQueueAllowed()
     */
    public boolean canJumpQueue()
    {
        return getOptions().isJumpQueueAllowed();
    }
}
