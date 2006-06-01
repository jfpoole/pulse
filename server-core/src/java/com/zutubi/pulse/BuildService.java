/********************************************************************************
 @COPYRIGHT@
 ********************************************************************************/
package com.zutubi.pulse;

import java.io.File;

/**
 */
public interface BuildService extends RemoteService
{
    /**
     * Returns true iff the service has the given version of the given
     * resource.
     *
     * @param resource the name of the required resource
     * @param version  the required version, or null if no specific version
     *                 is required
     * @return true iff this service has the give resource version
     */
    boolean hasResource(String resource, String version);

    boolean build(RecipeRequest request);

    void collectResults(long recipeId, File outputDest, File workDest);

    void cleanup(long recipeId);

    /**
     * Terminates the given recipe if it is still running.  This method may
     * only be called *after* receiving the recipe commenced event for the
     * recipe.
     *
     * @param recipeId the recipe to terminate
     */
    void terminateRecipe(long recipeId);

    // get available resources..... so that we can check to see if the
    // build host requirements are fullfilled.

    String getHostName();

}
