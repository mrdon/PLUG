package com.atlassian.plugin.osgi.container.impl;

import com.atlassian.plugin.osgi.container.OsgiPersistentCache;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.PluginException;
import com.sun.jmx.snmp.internal.SnmpEngineImpl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.Validate;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 * Default implementation of persistent cache.  Handles clearing of directories if an upgrade has been detected.
 *
 * @since 2.2.0
 */
public class DefaultOsgiPersistentCache implements OsgiPersistentCache
{
    private final File osgiBundleCache;
    private final File frameworkBundleCache;
    private final File transformedPluginCache;
    private final String applicationVersion;
    private Logger log = Logger.getLogger(DefaultOsgiPersistentCache.class);

    /**
     * @deprecated
     */
    @Deprecated
    public DefaultOsgiPersistentCache(File baseDir)
    {
        this(baseDir, null);
    }
    
    public DefaultOsgiPersistentCache(File baseDir, String applicationVersion)
    {
        Validate.notNull(baseDir, "The base directory for OSGi caches cannot be null");
        Validate.isTrue(baseDir.exists(), "The base directory for OSGi persistent caches should exist");
        osgiBundleCache = new File(baseDir, "felix");
        frameworkBundleCache = new File(baseDir, "framework-bundles");
        transformedPluginCache = new File(baseDir, "transformed-plugins");
        this.applicationVersion = applicationVersion;
        validate();
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

    private void validate()
    {
        ensureDirectoryExists(frameworkBundleCache);
        ensureDirectoryExists(osgiBundleCache);
        ensureDirectoryExists(transformedPluginCache);

        if (applicationVersion != null)
        {
            File versionFile = new File(osgiBundleCache, "host.version");
            if (versionFile.exists())
            {
                String oldVersion = null;
                try
                {
                    oldVersion = FileUtils.readFileToString(versionFile);
                }
                catch (IOException e)
                {
                    log.debug("Unable to read version file", e);
                }
                if (!applicationVersion.equals(oldVersion))
                {
                    log.info("Application upgrade detecting, clearing OSGi cache directories");
                    clear();
                }
                else
                {
                    return;
                }
            }

            try
            {
                FileUtils.writeStringToFile(versionFile, applicationVersion);
            }
            catch (IOException e)
            {
                log.warn("Unable to write cache version file, so will be unable to detect upgrades", e);
            }
        }
    }

    private void ensureDirectoryExists(File dir)
    {
        if (dir.exists() && !dir.isDirectory())
        {
            throw new IllegalArgumentException("'"+dir+"' is not a directory");
        }

        if (!dir.exists() && !dir.mkdir())
        {
            throw new IllegalArgumentException("Directory '"+dir+"' cannot be created");
        }
    }
}
