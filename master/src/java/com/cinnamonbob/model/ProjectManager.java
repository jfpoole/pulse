package com.cinnamonbob.model;

import java.util.List;

/**
 * 
 *
 */
public interface ProjectManager extends EntityManager<Project>
{
    /**
     * Looks up the project of the given name.
     *
     * @param name the name of the project to find
     * @return the relevant project, or null if not found
     */
    Project getProject(String name);

    Project getProject(long id);

    List<Project> getAllProjects();

    List<Project> getProjectsWithNameLike(String s);

    void initialise();

    /**
     * Deletes a build specification *and* all triggers that refer to it.
     *
     * @param projectId the identifier of the project to delete the
     *                  specification from
     * @param specId    the identifier of the specification to delete
     * @throws com.cinnamonbob.core.BobRuntimeException
     *          if there is no
     *          specification with the given identifier, or an error occurs
     *          while deleting it
     */
    void deleteBuildSpecification(long projectId, long specId);

    void buildCommenced(Project project);

    void buildCompleted(Project project);

    void pauseProject(Project project);

    void resumeProject(Project project);

    void delete(long projectId);
}
