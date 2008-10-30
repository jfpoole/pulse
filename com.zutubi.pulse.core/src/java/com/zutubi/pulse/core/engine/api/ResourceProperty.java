package com.zutubi.pulse.core.engine.api;

public class ResourceProperty
{
    private String name;
    private String value;
    private boolean addToEnvironment = false;
    private boolean addToPath = false;
    private boolean resolveVariables = false;

    public ResourceProperty()
    {
    }

    public ResourceProperty(String name, String value)
    {
        this(name, value, false, false, false);
    }

    public ResourceProperty(String name, String value, boolean addToEnvironment, boolean addToPath, boolean resolveVariables)
    {
        this.name = name;
        this.value = value;
        this.addToEnvironment = addToEnvironment;
        this.addToPath = addToPath;
        this.resolveVariables = resolveVariables;
    }

    public ResourceProperty copy()
    {
        return new ResourceProperty(getName(), value, addToEnvironment, addToPath, resolveVariables);
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }

    public boolean getAddToEnvironment()
    {
        return addToEnvironment;
    }

    public boolean getAddToPath()
    {
        return addToPath;
    }

    public boolean getResolveVariables()
    {
        return resolveVariables;
    }
}
