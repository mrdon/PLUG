package com.atlassian.plugin.webresource;

import java.io.Writer;

public interface PluginWebResourceManager
{
    /**
     * The mode in which the resource should be served. The modes
     * are described as follows:
     * Batch - Batch serve the web resources, grouped by the different file types.
     * Single - Serve each web resource individually.
     * Inline - Serve the contents of the web resource contents inline.
     */
    public enum RequestMode { BATCH, SINGLE, INLINE }

    /**
     * Indicate that a certain resource is required to be written out later with {@link #writeRequiredResources(Writer, RequestMode)}.
     *
     * @param resourceName The fully qualified plugin name to include (eg <code>jira.webresources:scriptaculous</code>)
     */
    public void requireResource(String resourceName);

    /**
     * Writes the previously requested resources (by {@link #requireResource(String)} out to the writer in the given
     * request mode.
     */
    public void writeRequiredResources(Writer writer, RequestMode mode);

    /**
     * Writes the given resource out to the writer in the given request mode.
     */
    public void writeResource(String resourceName, Writer writer, RequestMode mode);

    /**
     * A helper method to return a prefix for static web resources. It will be in the
     * following format:
     * <p/>
     * <pre><code>/s/{build num}/{system counter}/{resource counter}/_</code></pre>
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * <pre><code>&lt;%= request.getContextPath() %>/styles/global.css</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticResourcePrefix(resourceCounter) %>/styles/global.css</code></pre>
     *
     * @param resourceCounter A number that represents the unique version of the resource you require.  Every time this
     *                        resource changes, you need to increment the resource counter
     * @return A prefix that can be used to prefix static web resources.
     */
    public String getStaticResourcePrefix(String resourceCounter);
}
