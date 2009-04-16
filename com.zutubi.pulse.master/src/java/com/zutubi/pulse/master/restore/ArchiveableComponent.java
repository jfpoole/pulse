package com.zutubi.pulse.master.restore;

import java.io.File;

/**
 * The archiveable component represents a component whose state can be
 * exported to and imported from disk.
 *
 * The purpose of this interface is to support the backup and restore
 * process, and so should be implemented by the components whose state
 * is needed for the correct running of the application.
 */
public interface ArchiveableComponent
{
    /**
     * Unique identifier for this restorable component.
     *
     * @return a name for this archiveable instance
     */
    String getName();

    /**
     * A human readable description of the component
     *
     * @return a human readable description.
     */
    String getDescription();

    /**
     * Backup the persistent state to the specified directory.
     *
     * @param dir   the directory to write the persistent state to.
     *
     * @throws ArchiveException if the archive process encounters any problems.
     */
    void backup(File dir) throws ArchiveException;

    /**
     * Indicates if state for this component exists in the given persistent
     * state directory.
     *
     * @param dir directory to read the persistent state from
     * @return true iff data exists for this component in the given state
     *         directory
     */
    boolean exists(File dir);

    /**
     * Restore the persistent state from the specified directory.
     *
     * @param dir   the directory to read the persistent state from.
     *
     * @throws ArchiveException if the archive process encounters any problems.
     */
    void restore(File dir) throws ArchiveException;
}
