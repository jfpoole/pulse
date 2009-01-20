package com.zutubi.pulse.master.tove.format;

import com.zutubi.tove.config.api.Configuration;
import com.zutubi.tove.ConventionSupport;
import com.zutubi.tove.type.CompositeType;
import com.zutubi.tove.type.TypeRegistry;
import com.zutubi.util.bean.ObjectFactory;
import com.zutubi.util.logging.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides support for displaying state information for configuration
 * instances.
 */
public class StateDisplayManager
{
    private static final Logger LOG = Logger.getLogger(StateDisplayManager.class);

    private Map<CompositeType, StateDisplayFields> fieldsByType = new HashMap<CompositeType, StateDisplayFields>();
    private ObjectFactory objectFactory;
    private TypeRegistry typeRegistry;

    public List<String> getDisplayFields(Configuration configurationInstance)
    {
        if (configurationInstance != null && configurationInstance.isConcrete())
        {
            try
            {
                return getStateDisplayFields(getType(configurationInstance)).getFields(configurationInstance);
            }
            catch (Exception e)
            {
                LOG.severe(e);
            }
        }

        return Collections.emptyList();
    }

    public Object format(String fieldName, Configuration configurationInstance)
    {
        CompositeType type = getType(configurationInstance);
        StateDisplayFields displayFields = getStateDisplayFields(type);
        if (displayFields.hasField(fieldName))
        {
            try
            {
                return displayFields.format(fieldName, configurationInstance);
            }
            catch (Exception e)
            {
                LOG.severe(e);
                throw new RuntimeException(e);
            }
        }
        else
        {
            LOG.warning("Request for unrecognised display field '" + fieldName + "' on path '" + configurationInstance.getConfigurationPath() + "'");
        }

        return "<unable to format>";
    }

    private CompositeType getType(Object configurationInstance)
    {
        CompositeType type = typeRegistry.getType(configurationInstance.getClass());
        if (type == null)
        {
            throw new IllegalArgumentException("Invalid instance: not of configuration type");
        }
        return type;
    }

    public synchronized StateDisplayFields getStateDisplayFields(CompositeType type)
    {
        StateDisplayFields fields = fieldsByType.get(type);
        if (fields == null)
        {
            Class<? extends Configuration> configurationClass = type.getClazz();
            fields = new StateDisplayFields(configurationClass, ConventionSupport.getStateDisplay(configurationClass), objectFactory);
            fieldsByType.put(type, fields);
        }

        return fields;
    }

    public void setObjectFactory(ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }

    public void setTypeRegistry(TypeRegistry typeRegistry)
    {
        this.typeRegistry = typeRegistry;
    }
}