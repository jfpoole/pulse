package com.cinnamonbob.bootstrap;

import java.io.File;

/**
 * <class-comment/>
 */
public interface UserPaths
{
    /**
     * The user configuration directory.  Configuration files in this
     * directory are expected to be edited by the user.  A prime example is
     * basic system properties such as the port for the web application.
     *
     * @return the directory to use for user configuration files
     */
    File getUserConfigRoot();

    /**
     * The database root directory.  All database files are stored under this
     * root.
     *
     * @return the directory to use as the database root
     */
    File getDatabaseRoot();

    /**
     * The project root directory. All working files are
     *
     * @return the directory in which project build artifacts are stored.
     */
    File getProjectRoot();
}
