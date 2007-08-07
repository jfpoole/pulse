package com.zutubi.pulse.vfs.pulse;

import com.zutubi.pulse.servercore.scm.config.ScmConfiguration;
import org.apache.commons.vfs.FileSystemException;

/**
 * Provider for accessing Scm instances.
 */
public interface ScmProvider
{
    ScmConfiguration getScm() throws FileSystemException;
}
