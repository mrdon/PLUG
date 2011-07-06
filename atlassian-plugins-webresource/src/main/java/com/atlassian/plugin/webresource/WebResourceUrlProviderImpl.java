package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginInformation;

/**
 * The default implementation of {@link WebResourceUrlProvider}.
 *
 * @since 2.10
 */
public class WebResourceUrlProviderImpl implements WebResourceUrlProvider
{
    static final String STATIC_RESOURCE_PREFIX = "s";
    static final String STATIC_RESOURCE_SUFFIX = "_";

    private final WebResourceIntegration webResourceIntegration;

    public WebResourceUrlProviderImpl(WebResourceIntegration webResourceIntegration)
    {
        this.webResourceIntegration = webResourceIntegration;
    }

    public String getStaticResourcePrefix(UrlMode urlMode)
    {
        // "{base url}/s/{lang?}/{build num}/{system counter}/_"
        // {lang} is optional
        String lang = webResourceIntegration.getStaticResourceLocale();
        return webResourceIntegration.getBaseUrl(urlMode) + "/" + STATIC_RESOURCE_PREFIX + "/" +
                (lang != null ? lang + "/" : "") +
                webResourceIntegration.getSystemBuildNumber() + "/" + webResourceIntegration.getSystemCounter() + "/" + STATIC_RESOURCE_SUFFIX;
    }

    public String getStaticResourcePrefix(String resourceCounter, UrlMode urlMode)
    {
        // "{base url}/s/{lang?}/{build num}/{system counter}/{resource counter}/_"
        // {lang} is optional
        String lang = webResourceIntegration.getStaticResourceLocale();
        return webResourceIntegration.getBaseUrl(urlMode) + "/" + STATIC_RESOURCE_PREFIX + "/" +
                (lang != null ? lang + "/" : "") +
                webResourceIntegration.getSystemBuildNumber() + "/" + webResourceIntegration.getSystemCounter() + "/" + resourceCounter + "/" + STATIC_RESOURCE_SUFFIX;
    }

    public String getStaticPluginResourceUrl(final String moduleCompleteKey, final String resourceName, final UrlMode urlMode)
    {
        final ModuleDescriptor<?> moduleDescriptor = webResourceIntegration.getPluginAccessor().getEnabledPluginModule(moduleCompleteKey);
        if (moduleDescriptor == null)
        {
            return null;
        }

        return getStaticPluginResourceUrl(moduleDescriptor, resourceName, urlMode);
    }

    public String getStaticPluginResourceUrl(ModuleDescriptor moduleDescriptor, String resourceName, UrlMode urlMode)
    {
        PluginInformation pluginInfo = moduleDescriptor.getPlugin().getPluginInformation();
        String pluginVersion = pluginInfo != null ? pluginInfo.getVersion() : "unknown";

        // "{base url}/s/{build num}/{system counter}/{plugin version}/_"
        final String staticUrlPrefix = getStaticResourcePrefix(pluginVersion, urlMode);
        // "/download/resources/plugin.key:module.key/resource.name"
        return staticUrlPrefix + getResourceUrl(moduleDescriptor.getCompleteKey(), resourceName);
    }

    public String getResourceUrl(final String moduleCompleteKey, final String resourceName)
    {
        return new SinglePluginResource(resourceName, moduleCompleteKey, false).getUrl();
    }

    public String getBaseUrl()
    {
        return webResourceIntegration.getBaseUrl();
    }

    public String getBaseUrl(UrlMode urlMode)
    {
        return webResourceIntegration.getBaseUrl(urlMode);
    }
}
