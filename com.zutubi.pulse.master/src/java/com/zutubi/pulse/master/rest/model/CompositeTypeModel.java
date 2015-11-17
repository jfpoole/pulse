package com.zutubi.pulse.master.rest.model;

import com.zutubi.pulse.master.rest.model.forms.FormModel;
import com.zutubi.tove.type.CompositeType;
import com.zutubi.tove.type.TypeException;
import com.zutubi.tove.type.TypeProperty;
import com.zutubi.tove.type.record.MutableRecord;

import java.util.*;

/**
 * Model wrapping composite types.
 */
public class CompositeTypeModel extends TypeModel
{
    private List<PropertyModel> simpleProperties;
    private Map<String, Object> simplePropertyDefaults;
    private List<PropertyModel> nestedProperties;
    private List<CompositeTypeModel> subTypes;
    private FormModel form;
    private CompositeTypeModel checkType;

    public CompositeTypeModel()
    {
    }

    public CompositeTypeModel(CompositeType type)
    {
        super(type.getSymbolicName());

        List<String> simplePropertyNames = type.getSimplePropertyNames();
        if (simplePropertyNames.size() > 0)
        {
            simpleProperties = new ArrayList<>();
            for (String propertyName: simplePropertyNames)
            {
                TypeProperty property = type.getProperty(propertyName);
                simpleProperties.add(new PropertyModel(property));
            }
        }

        try
        {
            MutableRecord defaults = type.unstantiate(type.getDefaultInstance(), null);
            if (defaults != null)
            {
                Set<String> simpleKeySet = defaults.simpleKeySet();
                if (simpleKeySet.size() > 0)
                {
                    simplePropertyDefaults = new HashMap<>();
                    for (String key: simpleKeySet)
                    {
                        simplePropertyDefaults.put(key, defaults.get(key));
                    }
                }
            }
        }
        catch (TypeException e)
        {
            // Defaults are not essential.
        }

        List<String> nestedPropertyNames = type.getNestedPropertyNames();
        if (nestedPropertyNames.size() > 0)
        {
            nestedProperties = new ArrayList<>();
            for (String propertyName: nestedPropertyNames)
            {
                TypeProperty property = type.getProperty(propertyName);
                nestedProperties.add(new PropertyModel(property));
            }
        }
    }

    public List<PropertyModel> getSimpleProperties()
    {
        return simpleProperties;
    }

    public Map<String, Object> getSimplePropertyDefaults()
    {
        return simplePropertyDefaults;
    }

    public List<PropertyModel> getNestedProperties()
    {
        return nestedProperties;
    }

    public List<CompositeTypeModel> getSubTypes()
    {
        return subTypes;
    }

    public void addSubType(CompositeTypeModel subType)
    {
        if (subTypes == null)
        {
            subTypes = new ArrayList<>();
        }

        subTypes.add(subType);
    }

    public FormModel getForm()
    {
        return form;
    }

    public void setForm(FormModel form)
    {
        this.form = form;
    }

    public CompositeTypeModel getCheckType()
    {
        return checkType;
    }

    public void setCheckType(CompositeTypeModel checkType)
    {
        this.checkType = checkType;
    }

    public static class PropertyModel
    {
        private TypeProperty property;

        public PropertyModel(TypeProperty property)
        {
            this.property = property;
        }

        public String getName()
        {
            return property.getName();
        }

        public String getShortType()
        {
            return formatShortType(property.getType());
        }
    }
}