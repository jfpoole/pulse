package com.zutubi.pulse.services;

import com.zutubi.pulse.SystemInfo;
import com.zutubi.pulse.BuildContext;
import com.zutubi.pulse.resources.ResourceConstructor;
import com.zutubi.pulse.filesystem.FileInfo;
import com.zutubi.pulse.core.RecipeRequest;
import com.zutubi.pulse.core.model.Resource;
import com.zutubi.pulse.logging.CustomLogRecord;

import java.util.List;

/**
 */
public interface SlaveService
{
    /**
     * Most primitive communication, do *not* change the signature of this
     * method.
     *
     * @return the build number of the slave (we will only continue to talk
     *         if the build number matches ours)
     */
    int ping();

    /**
     * The update mechanism needs to be stable.  Any changes to the way this
     * works requires knowledge in new code (master and slave side) that
     * knows to veto impossible updates.
     *
     * @param token secure token for inter-agent communication
     * @param build the build number to update to
     * @param master url of the master requesting the update
     * @param id the slave's id, for when it calls us back
     * @param packageUrl URL from which a zip containing the given build can
     *                   be obtained
     * @return true if the agent wishes to proceed with the update
     */
    boolean updateVersion(String token, String build, String master, long id, String packageUrl, long packageSize);

    SlaveStatus getStatus(String token, String master);

    /**
     * A request to build a recipe on the slave, if the slave is currently idle.
     *
     * @param token   secure token for inter-agent communication
     * @param master  location of the master for return messages
     * @param slaveId id of the slave, used in returned messages
     * @param request details of the recipe to build
     * @return true if the request was accepted, false of the slave was busy
     *
     * @throws InvalidTokenException if the given token does not match the
     * slave's
     */
    boolean build(String token, String master, long slaveId, RecipeRequest request, BuildContext context) throws InvalidTokenException;

    void cleanupRecipe(String token, String project, String spec, long recipeId, boolean incremental) throws InvalidTokenException;

    void terminateRecipe(String token, long recipeId) throws InvalidTokenException;

    SystemInfo getSystemInfo(String token) throws InvalidTokenException;

    List<CustomLogRecord> getRecentMessages(String token) throws InvalidTokenException;

    List<Resource> discoverResources(String token);

    /*
    String[] list(String path);
    */
    
    FileInfo getFileInfo(String token, String path);

    String[] listRoots(String token);

    Resource createResource(ResourceConstructor constructor, String path);

    boolean isResourceHome(ResourceConstructor constructor, String path);
}