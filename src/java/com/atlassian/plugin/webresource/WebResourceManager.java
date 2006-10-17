package com.atlassian.plugin.webresource;

import java.io.Writer;
import java.io.IOException;

/**
 * Manage 'css', 'javascript' and other 'resources' that are usually linked at the top of pages using
 * <code>&lt;script&gt;</code> and <code>&lt;link&gt; tags.
 * <p>
 * By using the WebResourceManager, components can declare dependencies on javascript and css that they would
 * otherwise have to embed inline (which means that it can't be cached, and are often duplicated in a page).
 */
public interface WebResourceManager
{
    /**
     * Called by a component to indicate that a certain resource is required to be inserted into
     * this page.
     *
     * @param resourceName The fully qualified plugin name to include (eg <code>jira.webresources:scriptaculous</code>)
     */
    public void requireResource(String resourceName);

    /**
     * Include the resources that have already been specified by the request in the page.  This is done by including
     * links to the resources that have been specified.
     * <p>
     * Example - if a 'javascript' resource has been specified, this method should output:
     * <pre><code>
     *  &lt;script type=&quot;text/javascript&quot; src=&quot;$contextPath/scripts/javascript.js&quot;&gt;&lt;/script&gt;
     * </code></pre>
     * Similarly for other supported resources
     *
     * @param contextPath   For linking to resources, we need to specify the context path
     * @param writer    The writer to write the links to
     * @throws java.io.IOException  If the {@link Writer} passed in throws an exception when being written to
     */
    public void includeResources(String contextPath, Writer writer) throws IOException;
}
