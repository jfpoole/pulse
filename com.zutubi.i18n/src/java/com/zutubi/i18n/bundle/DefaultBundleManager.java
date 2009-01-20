package com.zutubi.i18n.bundle;

import com.zutubi.i18n.context.*;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * <class-comment/>
 */
public class DefaultBundleManager implements BundleManager
{
    private ResourceBundleFactory factory = new BaseResourceBundleFactory();

    private ContextCache cache;

    private final Map<Class<? extends Context>, List<ContextResolver>> resolvers = new HashMap<Class<? extends Context>, List<ContextResolver>>();

    private ContextLoader defaultLoader = new DefaultContextLoader();

    public DefaultBundleManager()
    {
    }

    public DefaultBundleManager(ContextCache cache)
    {
        this.cache = cache;
    }

    public void addResolver(ContextResolver resolver)
    {
        synchronized(resolvers)
        {
            Class clazz = resolver.getContextType();
            if (!resolvers.containsKey(clazz))
            {
                resolvers.put(clazz, new LinkedList<ContextResolver>());
            }
            List<ContextResolver> list = resolvers.get(clazz);
            list.add(resolver);
        }
    }

    public List<ResourceBundle> getBundles(Context context, Locale locale)
    {
        if (cache.isCached(context, locale))
        {
            return cache.getFromCache(context, locale);
        }

        List<ResourceBundle> bundles = new LinkedList<ResourceBundle>();
        List<ContextResolver> resolvers = new LinkedList<ContextResolver>();
        List<String> bundleNames = new LinkedList<String>();

        synchronized(this.resolvers)
        {
            resolvers.addAll(this.resolvers.get(context.getClass()));
        }
        
        for (ContextResolver resolver : resolvers)
        {
            bundleNames.addAll(Arrays.asList(resolver.resolve(context)));
        }

        bundleNames = CollectionUtils.unique(bundleNames);

        for (String bundleName : bundleNames)
        {
            List<String> candidateNames = factory.expand(bundleName, locale);
            for (String candidateName : candidateNames)
            {
                InputStream input = null;
                try
                {
                    input = getContextLoader(context).loadResource(context, candidateName);
                    if (input != null)
                    {
                        bundles.add(factory.loadBundle(input, locale));
                    }
                }
                catch (IOException e)
                {
                    // noop.
                }
                finally
                {
                    IOUtils.close(input);
                }
            }
        }

        cache.addToCache(context, locale, bundles);

        return bundles;

    }

    private ContextLoader getContextLoader(Context context)
    {
        return defaultLoader;
    }

    public void setContextCache(ContextCache cache)
    {
        this.cache = cache;
    }

    public void clearContextCache()
    {
        this.cache.clear();
    }
}