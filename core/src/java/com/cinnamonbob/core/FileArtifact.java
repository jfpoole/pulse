package com.cinnamonbob.core;

import com.cinnamonbob.core.validation.Validateable;
import com.opensymphony.xwork.validator.ValidatorContext;

import java.io.*;

/**
 * 
 *
 */
public class FileArtifact implements Validateable
{
    private File file;
    private File toFile;
    private String name;
    private String title;
    private String type;

    public FileArtifact(String name, File file)
    {
        this.name = name;
        this.file = file;
    }

    public FileArtifact()
    {

    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setFile(File file)
    {
        this.file = file;
    }

    public InputStream getContent()
    {
        if (!file.exists())
        {
            return new ByteArrayInputStream(new byte[0]);
        }

        try
        {
            return new FileInputStream(file);
        }
        catch (FileNotFoundException e)
        {
            // will not get here since we have checked that the file exists.
        }
        return null;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setFromFile(File f)
    {
        setFile(f);
    }

    public File getToFile()
    {
        return toFile;
    }

    public void setToFile(File f)
    {
        toFile = f;
    }

    public String getName()
    {
        return name;
    }

    public String getTitle()
    {
        return title;
    }

    public String getType()
    {
        return type;
    }

    public File getFile()
    {
        return file;
    }

    public void validate(ValidatorContext context)
    {
        if (getToFile() != null && getToFile().isAbsolute())
        {
            context.addFieldError("toFile", "The toFile attribute can not be absolute. " +
                    "Please specify a file relative location.");
        }
    }
}
