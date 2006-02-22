package com.cinnamonbob.bootstrap.velocity;

import com.cinnamonbob.bootstrap.ConfigUtils;
import com.cinnamonbob.bootstrap.SystemPaths;
import com.cinnamonbob.util.logging.Logger;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;

import java.io.File;

/**
 * A velocity resource file loader that uses the application installation
 * directories to lookup velocity templates.
 *
 */
public class BobTemplateVelocityResourceLoader extends FileResourceLoader
{
    private static final Logger LOG = Logger.getLogger(BobTemplateVelocityResourceLoader.class);

    /**
     * Retrieve a comma separated list of the systems template paths. These paths
     * are represented as absolute paths.
     *
     */
    public static String getFullTemplatePath()
    {
        SystemPaths paths = ConfigUtils.getManager().getSystemPaths();

        StringBuffer result = new StringBuffer();
        String sep = "";
        for (File path : paths.getTemplateRoots())
        {
            result.append(sep);
            result.append(path.getAbsolutePath());
            sep = ",";
        }
        return result.toString();
    }

    public void init(ExtendedProperties configuration)
    {
        // NOTE: the path can be a comma separated list of paths... 
        configuration.setProperty("path", getFullTemplatePath());
        super.init(configuration);
    }

}
