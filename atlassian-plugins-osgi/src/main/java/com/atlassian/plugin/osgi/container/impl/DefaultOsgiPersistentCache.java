package com.atlassian.plugin.osgi.container.impl;

import com.atlassian.plugin.osgi.container.OsgiPersistentCache;
import com.atlassian.plugin.osgi.container.OsgiContainerException;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.Validate;
import org.apache.commons.io.FileUtils;

/**
 * Default implementation of persistent cache.  Of the two constructors, {@link #DefaultOsgiPersistentCache(File)} is
 * recommended to help standardize cache directory names
 *
 * @since 2.2.0
 */
public class DefaultOsgiPersistentCache implements OsgiPersistentCache
{
    private final File osgiBundleCache;
    private final File frameworkBundleCache;
    private final File transformedPluginCache;

    public DefaultOsgiPersistentCache(File baseDir)
    {
        Validate.isTrue(baseDir.exists(), "The base directory for OSGi persistent caches should exist");
        osgiBundleCache = new File(baseDir, "felix");
        frameworkBundleCache = new File(baseDir, "framework-bundles");
        transformedPluginCache = new File(baseDir, "transformed-plugins");
    }

    public DefaultOsgiPersistentCache(File osgiBundleCache, File frameworkBundleCache, File transformedPluginCache)
    {
        this.osgiBundleCache = osgiBundleCache;
        this.frameworkBundleCache = frameworkBundleCache;
        this.transformedPluginCache = transformedPluginCache;
    }


    public File getFrameworkBundleCache()
    {
        return frameworkBundleCache;
    }

    public File getOsgiBundleCache()
    {
        return osgiBundleCache;
    }

    public File getTransformedPluginCache()
    {
        return transformedPluginCache;
    }

    public void clear() throws OsgiContainerException
    {
        try
        {
            FileUtils.cleanDirectory(frameworkBundleCache);
            FileUtils.cleanDirectory(osgiBundleCache);
            FileUtils.cleanDirectory(transformedPluginCache);
        }
        catch (IOException e)
        {
            throw new OsgiContainerException("Unable to clear OSGi caches", e);
        }
    }
}
