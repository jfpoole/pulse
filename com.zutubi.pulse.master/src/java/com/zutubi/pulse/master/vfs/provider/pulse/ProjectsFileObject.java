package com.zutubi.pulse.master.vfs.provider.pulse;

import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.webwork.Urls;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.provider.AbstractFileSystem;

/**
 * <class comment/>
 */
public class ProjectsFileObject extends AbstractPulseFileObject implements AddressableFileObject
{
    public ProjectsFileObject(final FileName name, final AbstractFileSystem fs)
    {
        super(name, fs);
    }

    public AbstractPulseFileObject createFile(FileName fileName) throws Exception
    {
        // the fileName may be project name or the project id.
        long projectId = convertToProjectId(fileName.getBaseName());
        if (projectId != -1)
        {
            // Within the pulse file system, we use the projects id, not the projects name. So
            // lets reconstruct the adjusted name.
            String absPath = fileName.getParent().getPath() + "/" + projectId;

            fileName = new PulseFileName(fileName.getScheme(), absPath, fileName.getType());

            return objectFactory.buildBean(ProjectFileObject.class,
                    new Class[]{FileName.class, Long.TYPE, AbstractFileSystem.class},
                    new Object[]{fileName, projectId, pfs}
            );
        }
        
        // we need to return a place holder here.
        return null;
    }

    private long convertToProjectId(String str)
    {
        try
        {
            return Long.parseLong(str);
        }
        catch (NumberFormatException e)
        {
            ProjectConfiguration project = projectManager.getProjectConfig(str, true);
            if (project != null)
            {
                return project.getProjectId();
            }
        }
        return -1;
    }

    protected FileType doGetType() throws Exception
    {
        // allow traversal of this node.
        return FileType.FOLDER;
    }

    protected String[] doListChildren() throws Exception
    {
        // do not support listing.
        return new String[0];
    }

    public boolean isLocal()
    {
        return true;
    }

    public String getUrlPath()
    {
        return Urls.getBaselessInstance().projects();
    }
}