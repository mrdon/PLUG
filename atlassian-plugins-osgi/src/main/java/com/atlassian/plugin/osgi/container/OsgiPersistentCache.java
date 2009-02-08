package com.atlassian.plugin.osgi.container;

import java.io.File;

/**
 * Access to persistent cache locations used throughout the OSGi plugin system
 *
 * @since 2.2.0
 */
public interface OsgiPersistentCache
{
    /**
     * @return the directory to store extracted framework bundles
     */
    File getFrameworkBundleCache();

    /**
     * @return the directory to use for the container bundle cache
     */
    File getOsgiBundleCache();

    /**
     * @return the directory to store transformed plugins
     */
    File getTransformedPluginCache();

    /**
     * Clear all caches
     *
     * @throws OsgiContainerException If the caches couldn't be cleared
     */
    void clear() throws OsgiContainerException;
}
