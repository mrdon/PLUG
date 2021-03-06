package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.RESOURCE_URL_PREFIX;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;
import com.atlassian.plugin.Plugin;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a single plugin resource.
 *
 * It provides methods to parse and generate urls to locate a single plugin resource.
 *
 * Note: This PluginResource does not use it's parameters in generating the url. 
 *
 * @since 2.2
 */
public class SinglePluginResource implements PluginResource
{
    /**
     * The url prefix to a single plugin resource: "/download/resources/"
     */
    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + RESOURCE_URL_PREFIX;

    private final String resourceName;
    private final String moduleCompleteKey;
    private final boolean cached;
    private final Map<String, String> params;

    public SinglePluginResource(final String resourceName, final String moduleCompleteKey, final boolean cached)
    {
        this(resourceName, moduleCompleteKey, cached, Collections.<String, String>emptyMap());
    }

    public SinglePluginResource(final String resourceName, final String moduleCompleteKey, final boolean cached, final Map<String, String> params)
    {
        this.resourceName = resourceName;
        this.moduleCompleteKey = moduleCompleteKey;
        this.cached = cached;
        this.params = ImmutableMap.copyOf(params);
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public String getModuleCompleteKey()
    {
        return moduleCompleteKey;
    }

    public Map<String, String> getParams()
    {
        return params;
    }

    public String getVersion(WebResourceIntegration integration)
    {
        final Plugin plugin = integration.getPluginAccessor().getEnabledPluginModule(getModuleCompleteKey()).getPlugin();
        return plugin.getPluginInformation().getVersion();
    }

    public boolean isCacheSupported()
    {
        return cached;
    }

    /**
     * Returns a url string in the format: /download/resources/MODULE_COMPLETE_KEY/RESOURCE_NAME
     *
     * e.g. /download/resources/example.plugin:webresources/foo.css
     */
    public String getUrl()
    {
        return URL_PREFIX + PATH_SEPARATOR + moduleCompleteKey + PATH_SEPARATOR + resourceName;
    }
}
