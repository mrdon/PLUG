package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadableResource;

/**
 * Finds a resource
 */
interface DownloadableResourceFinder
{
    /**
     * Finds a resource to upload given a module key and resource name
     * If the resource cannot be found null is returned.
     * @param moduleKey - the key of the web resource
     * @param resourceName - the name or the resource inside the given web resource
     * @return a resource to upload
     */
    public DownloadableResource find(String moduleKey, String resourceName);
}
