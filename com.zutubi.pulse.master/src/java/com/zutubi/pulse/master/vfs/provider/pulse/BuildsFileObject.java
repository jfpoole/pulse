package com.zutubi.pulse.master.vfs.provider.pulse;

import com.zutubi.pulse.core.model.EntityToIdMapping;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.CompositeMapping;
import com.zutubi.util.ToStringMapping;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.provider.AbstractFileSystem;

import java.util.List;

/**
 * Represents a collection of build results.  Sometimes scoped within a single
 * project, but not necessarily.
 */
public class BuildsFileObject extends AbstractPulseFileObject
{
    private static final int MAX_BUILDS = 10;

    public BuildsFileObject(final FileName name, final AbstractFileSystem fs)
    {
        super(name, fs);
    }

    public AbstractPulseFileObject createFile(final FileName fileName) throws FileSystemException
    {
        long buildId = getBuildId(fileName.getBaseName());
        if (buildId != -1)
        {
            return objectFactory.buildBean(BuildFileObject.class,
                    new Class[]{FileName.class, Long.TYPE, AbstractFileSystem.class},
                    new Object[]{fileName, buildId, pfs}
            );
        }
        // need an error place holder.
        return null;
    }

    private long getBuildId(String str) throws FileSystemException
    {
        long id = Long.parseLong(str);

        ProjectProvider provider = getAncestor(ProjectProvider.class);
        if (provider != null)
        {
            Project project = provider.getProject();

            BuildResult result = buildManager.getByProjectAndNumber(project, id);
            if (result != null)
            {
                return result.getId();
            }
        }

        BuildResult result = buildManager.getBuildResult(id);
        if (result != null)
        {
            return id;
        }
        return -1;
    }

    protected FileType doGetType() throws Exception
    {
        // this object does allow traversal.
        return FileType.FOLDER;
    }

    protected String[] doListChildren() throws Exception
    {
        ProjectProvider provider = getAncestor(ProjectProvider.class);
        if (provider != null)
        {
            Project project = provider.getProject();

            List<BuildResult> builds = buildManager.getLatestBuildResultsForProject(project, MAX_BUILDS);
            List<String> buildIds = CollectionUtils.map(builds, 
                    new CompositeMapping<BuildResult, Long, String>(new EntityToIdMapping<BuildResult>(), new ToStringMapping<Long>())
            );
            return buildIds.toArray(new String[buildIds.size()]);
        }

        return NO_CHILDREN;
    }
}