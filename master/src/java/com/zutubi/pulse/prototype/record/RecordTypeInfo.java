package com.zutubi.pulse.prototype.record;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.lang.annotation.Annotation;

/**
 * Holds information about a type in the record type registry, including the
 * property getter and setter methods.
 */
public class RecordTypeInfo
{
    private String symbolicName;
    private Class type;
    private Map<String, RecordPropertyInfo> properties = new HashMap<String, RecordPropertyInfo>();
    private List<Annotation> annotations  = new LinkedList<Annotation>();
    
    private Map<Class, List<? extends RecordPropertyInfo>> typeMap = new HashMap<Class, List<? extends RecordPropertyInfo>>();

    /**
     * The extensions are RecordTypeInfo objects that extend this record type info, in a similar way that
     * a class can be extended by a subclass. Where ever this RecordTypeInfo is used, one of its extensions
     * can also be used.
     */
    private List<RecordTypeInfo> extensions = new LinkedList<RecordTypeInfo>();

    public RecordTypeInfo(String symbolicName, Class type) throws InvalidRecordTypeException
    {
        this.symbolicName = symbolicName;
        this.type = type;
    }

    public String getSymbolicName()
    {
        return symbolicName;
    }

    public Class getType()
    {
        return type;
    }

    public RecordPropertyInfo getProperty(String name)
    {
        return properties.get(name);
    }

    <T extends RecordPropertyInfo> void addProperty(T propertyInfo)
    {
        properties.put(propertyInfo.getName(), propertyInfo);

        if (!typeMap.containsKey(propertyInfo.getClass()))
        {
            typeMap.put(propertyInfo.getClass(), new LinkedList<T>());
        }
        List<T> recordPropertyInfos = (List<T>) typeMap.get(propertyInfo.getClass());
        recordPropertyInfos.add(propertyInfo);
    }

    public Iterable<? extends RecordPropertyInfo> getProperties()
    {
        return properties.values();
    }

    public List<Annotation> getAnnotations()
    {
        return annotations;
    }

    public void addAnnotations(List<Annotation> annotations)
    {
        this.annotations.addAll(annotations);
    }

    public void addExtension(RecordTypeInfo typeInfo)
    {
        this.extensions.add(typeInfo);
    }

    public void addExtensions(List<RecordTypeInfo> typeInfos)
    {
        this.extensions.addAll(typeInfos);
    }

    public List<RecordTypeInfo> getExtensions()
    {
        return this.extensions;
    }

    public boolean hasExtensions()
    {
        return this.extensions.size() > 0;
    }

    public List<SimpleRecordPropertyInfo> getSimpleInfos()
    {
        return (List<SimpleRecordPropertyInfo>) typeMap.get(SimpleRecordPropertyInfo.class);
    }

    public List<ValueListRecordPropertyInfo> getValueListInfos()
    {
        return (List<ValueListRecordPropertyInfo>) typeMap.get(ValueListRecordPropertyInfo.class);
    }

    public List<SubrecordRecordPropertyInfo> getSubrecordInfos()
    {
        return (List<SubrecordRecordPropertyInfo>) typeMap.get(SubrecordRecordPropertyInfo.class);
    }
}
