package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;

import java.util.Map;

/**
 * The integration layer between Plugin's Web Resource Handler, and specific applications (eg JIRA, Confluence).
 *
 * @see WebResourceManagerImpl#WebResourceManagerImpl(WebResourceIntegration)
 */
public interface WebResourceIntegration
{
    /**
     * Applications must implement this method to get access to the application's PluginAccessor
     */
    public PluginAccessor getPluginAccessor();

    /**
     * This must be a thread-local cache that will be accessable from both the page, and the decorator
     */
    public Map getRequestCache();

    /**
     * Represents the unique number for this system, which when updated will flush the cache. This should be a number
     * and is generally stored in the global application-properties.
     *
     * @return A string representing the count
     */
    public String getSystemCounter();

    /**
     * Represents the last time the system was updated.  This is generally obtained from BuildUtils or similar.
     */
    public String getSystemBuildNumber();

    /**
     * This should be the 'short' contextPath for the system.  In most cases, this would equal request.getContextPath()
     * (perhaps retrieved from a thread local), or else parsed from the application's base URL.
     */
    public String getBaseUrl();
}
