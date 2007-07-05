package com.zutubi.pulse.model.persistence;

import com.zutubi.pulse.model.*;

import java.util.List;

/**
 * 
 *
 */
public interface ProjectDao extends EntityDao<Project>
{
    void save(TagPostBuildAction action);

    void save(RunExecutablePostBuildAction action);

    TagPostBuildAction findTagPostBuildAction(long id);

    RunExecutablePostBuildAction findRunExecutablePostBuildAction(long id);

    List<Project> findByAdminAuthority(String authority);

    List<Project> findAllProjectsCached();

    void delete(BuildHostRequirements hostRequirements);
}
