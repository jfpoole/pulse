package com.zutubi.pulse.vfs.pulse;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.provider.AbstractFileSystem;
import com.zutubi.pulse.core.model.RecipeResult;
import com.zutubi.pulse.model.BuildResult;
import com.zutubi.pulse.model.RecipeResultNode;

/**
 * <class comment/>
 */
public class NamedStageFileObject extends AbstractPulseFileObject implements RecipeResultProvider
{
    private final String recipeName;

    public NamedStageFileObject(final FileName name, final String recipeName, final AbstractFileSystem fs)
    {
        super(name, fs);

        this.recipeName = recipeName;
    }

    public AbstractPulseFileObject createFile(final FileName fileName) throws Exception
    {
        String name = fileName.getBaseName();
        return objectFactory.buildBean(NamedCommandFileObject.class,
                new Class[]{FileName.class, String.class, AbstractFileSystem.class},
                new Object[]{fileName, name, pfs}
        );
    }

    protected FileType doGetType() throws Exception
    {
        // support navigation but not listing for now.
        return FileType.FOLDER;
    }

    protected String[] doListChildren() throws Exception
    {
        // do not support listing for now.
        return new String[0];
    }

    public RecipeResult getRecipeResult() throws FileSystemException
    {
        BuildResult result = getBuildResult();
        if (result == null)
        {
            throw new FileSystemException("No build result available.");
        }
        
        RecipeResultNode node = result.getRoot().findNode(recipeName);
        if (node == null)
        {
            throw new FileSystemException(String.format("No recipe by the name '%s' is available.", recipeName));
        }
        
        RecipeResult recipeResult = node.getResult();
        if (recipeResult == null)
        {
            throw new FileSystemException("No recipe result is available.");
        }
        return recipeResult;
    }

    public long getRecipeResultId() throws FileSystemException
    {
        return getRecipeResult().getId();
    }

    protected BuildResult getBuildResult() throws FileSystemException
    {
        BuildResultProvider provider = (BuildResultProvider) getAncestor(BuildResultProvider.class);
        if (provider == null)
        {
            throw new FileSystemException("Missing build result context.");
        }
        return provider.getBuildResult();
    }
}