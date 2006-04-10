package com.zutubi.pulse;

import com.zutubi.pulse.core.BobException;
import com.zutubi.pulse.core.Bootstrapper;
import com.zutubi.pulse.core.BuildException;
import com.zutubi.pulse.core.RecipePaths;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 */
public class ChainBootstrapper implements Bootstrapper
{
    private List<Bootstrapper> bootstrappers = new LinkedList<Bootstrapper>();

    public ChainBootstrapper(Bootstrapper ...bootstrappers)
    {
        this.bootstrappers.addAll(Arrays.asList(bootstrappers));
    }

    public void prepare() throws BobException
    {
        for (Bootstrapper bootstrapper : bootstrappers)
        {
            bootstrapper.prepare();
        }
    }

    public void bootstrap(long recipeId, RecipePaths paths) throws BuildException
    {
        for (Bootstrapper bootstrapper : bootstrappers)
        {
            bootstrapper.bootstrap(recipeId, paths);
        }
    }

    public ChainBootstrapper add(Bootstrapper bootstrapper)
    {
        bootstrappers.add(bootstrapper);
        return this;
    }
}
