package com.zutubi.pulse.model.persistence;

import com.zutubi.pulse.model.BuildHostRequirements;
import com.zutubi.pulse.model.BuildSpecification;
import com.zutubi.pulse.model.Slave;

import java.util.List;

/**
 * <class-comment/>
 */
public interface BuildSpecificationDao extends EntityDao<BuildSpecification>
{
    List<BuildSpecification> findBySlave(Slave slave);

    void delete(BuildHostRequirements hostRequirements);
}