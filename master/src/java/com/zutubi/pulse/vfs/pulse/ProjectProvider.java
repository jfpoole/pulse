package com.zutubi.pulse.vfs.pulse;

import com.zutubi.pulse.model.Project;
import org.apache.commons.vfs.FileSystemException;

/**
 * A provider interface that indicates the current node represents a project instance.
 *
 * @see com.zutubi.pulse.model.Project
 */
public interface ProjectProvider
{
    Project getProject() throws FileSystemException;

    long getProjectId() throws FileSystemException;
}