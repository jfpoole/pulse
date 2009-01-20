package com.zutubi.pulse.core.scm;

import com.zutubi.pulse.core.personal.PersonalBuildException;
import com.zutubi.pulse.core.scm.api.WorkingCopy;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.TreeMap;

/**
 */
public class WorkingCopyFactory
{
    private static Map<String, Constructor> typeMap = new TreeMap<String, Constructor>();

    public static WorkingCopy create(String type) throws PersonalBuildException
    {
        Constructor constructor = typeMap.get(type);
        if(constructor != null)
        {
            try
            {
                return (WorkingCopy) constructor.newInstance();
            }
            catch (Exception e)
            {
                if (e instanceof PersonalBuildException)
                {
                    throw (PersonalBuildException)e;
                }
                if (e.getCause() instanceof PersonalBuildException)
                {
                    throw (PersonalBuildException)e.getCause();
                }
                return null;
            }
        }

        return null;
    }

    public static void registerType(String type, Class clazz) throws IllegalArgumentException, NoSuchMethodException
    {
        if(!WorkingCopy.class.isAssignableFrom(clazz))
        {
            throw new IllegalArgumentException("Class '" + clazz.getName() + "' does not implement WorkingCopy");
        }

        Constructor constructor = clazz.getConstructor();
        typeMap.put(type, constructor);
    }
}