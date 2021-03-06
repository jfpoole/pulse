/* Copyright 2017 Zutubi Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zutubi.tove.ui.model;

import com.zutubi.tove.config.docs.TypeDocs;
import com.zutubi.tove.type.CompositeType;
import com.zutubi.tove.type.TypeException;
import com.zutubi.tove.type.TypeProperty;
import com.zutubi.tove.type.record.MutableRecord;
import com.zutubi.tove.ui.model.forms.FormModel;

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
    private TypeDocs docs;

    public CompositeTypeModel()
    {
    }

    public CompositeTypeModel(CompositeType type)
    {
        super(type.getSymbolicName());

        simpleProperties = createProperties(type, type.getSimplePropertyNames());

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
                        TypeProperty property = type.getProperty(key);
                        if (property != null)
                        {
                            simplePropertyDefaults.put(key, property.getType().toXmlRpc(null, defaults.get(key)));
                        }
                    }
                }
            }
        }
        catch (TypeException e)
        {
            // Defaults are not essential.
        }

        nestedProperties = createProperties(type, type.getNestedPropertyNames());
    }

    private List<PropertyModel> createProperties(CompositeType type, List<String> propertyNames)
    {
        List<PropertyModel> properties = null;
        if (propertyNames.size() > 0)
        {
            properties = new ArrayList<>();
            for (String propertyName: propertyNames)
            {
                TypeProperty property = type.getProperty(propertyName);
                properties.add(new PropertyModel(property));
            }
        }

        return properties;
    }

    public List<PropertyModel> getSimpleProperties()
    {
        return simpleProperties;
    }

    public Map<String, Object> getSimplePropertyDefaults()
    {
        return simplePropertyDefaults;
    }

    public void addSimplePropertyDefault(String name, Object value)
    {
        if (simplePropertyDefaults == null)
        {
            simplePropertyDefaults = new HashMap<>();
        }

        simplePropertyDefaults.put(name, value);
    }

    public void setSimplePropertyDefaults(Map<String, Object> simplePropertyDefaults)
    {
        this.simplePropertyDefaults = simplePropertyDefaults;
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

    public TypeDocs getDocs()
    {
        return docs;
    }

    public void setDocs(TypeDocs docs)
    {
        this.docs = docs;
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
