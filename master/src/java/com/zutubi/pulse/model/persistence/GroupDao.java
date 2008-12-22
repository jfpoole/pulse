package com.zutubi.pulse.model.persistence;

import com.zutubi.pulse.model.Group;
import com.zutubi.pulse.model.User;

import java.util.List;

/**
 */
public interface GroupDao extends EntityDao<Group>
{
    Group findByName(String name);

    List<Group> findByAdminAllProjects();

    List<Group> findByMember(User member);
}