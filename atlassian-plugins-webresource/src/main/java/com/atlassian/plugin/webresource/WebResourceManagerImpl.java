package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * A handy super-class that handles most of the resource management.
 * <p/>
 * To use this manager, you need to have the following UrlRewriteFilter code:
 * <pre>
 * &lt;rule>
 * &lt;from>^/s/(.*)/_/(.*)&lt;/from>
 * &lt;run class="com.atlassian.plugin.servlet.ResourceDownloadUtils" method="addCachingHeaders" />
 * &lt;to type="forward">/$2&lt;/to>
 * &lt;/rule>
 * </pre>
 * <p/>
 * Sub-classes should implement the abstract methods
 */
public class WebResourceManagerImpl implements WebResourceManager
{
    private static final Log log = LogFactory.getLog(WebResourceManagerImpl.class);

    static final String STATIC_RESOURCE_PREFIX = "s";
    static final String STATIC_RESOURCE_SUFFIX = "_";

    static final String REQUEST_CACHE_RESOURCE_KEY = "plugin.webresource.names";

    protected final WebResourceIntegration webResourceIntegration;
    protected final PluginResourceLocator pluginResourceLocator;
    protected final ResourceBatchingConfiguration batchingConfiguration;
    protected static final List<WebResourceFormatter> webResourceFormatters = Arrays.asList(CssWebResource.FORMATTER, JavascriptWebResource.FORMATTER);

    public WebResourceManagerImpl(PluginResourceLocator pluginResourceLocator, WebResourceIntegration webResourceIntegration)
    {
        this(pluginResourceLocator,  webResourceIntegration, new DefaultResourceBatchingConfiguration());
    }

    public WebResourceManagerImpl(final PluginResourceLocator pluginResourceLocator, final WebResourceIntegration webResourceIntegration, ResourceBatchingConfiguration batchingConfiguration)
    {
        this.pluginResourceLocator = pluginResourceLocator;
        this.webResourceIntegration = webResourceIntegration;
        this.batchingConfiguration = batchingConfiguration;
    }

    public void requireResource(final String moduleCompleteKey)
    {
        log.debug("Requiring resource: " + moduleCompleteKey);
        final LinkedHashSet<String> webResourceNames = getWebResourceNames();

        final LinkedHashSet<String> resources = new LinkedHashSet<String>();
        addResourceWithDependencies(moduleCompleteKey, resources, new Stack<String>());
        webResourceNames.addAll(resources);
    }

    private LinkedHashSet<String> getWebResourceNames()
    {
        final Map<String, Object> cache = webResourceIntegration.getRequestCache();
        @SuppressWarnings("unchecked")
        LinkedHashSet<String> webResourceNames = (LinkedHashSet<String>) cache.get(REQUEST_CACHE_RESOURCE_KEY);
        if (webResourceNames == null)
        {
            webResourceNames = new LinkedHashSet<String>();
            cache.put(REQUEST_CACHE_RESOURCE_KEY, webResourceNames);
        }
        return webResourceNames;
    }

    /**
     * Adds the resources as well as its dependencies in order to the given set. This method uses recursion
     * to add a resouce's dependent resources also to the set. You should call this method with a new stack
     * passed to the last parameter.
     *
     * @param moduleKey the module complete key to add as well as its dependencies
     * @param orderedResourceKeys an ordered list set where the resources are added in order
     * @param stack where we are in the dependency tree
     */
    private void addResourceWithDependencies(final String moduleKey, final LinkedHashSet<String> orderedResourceKeys, final Stack<String> stack)
    {
        if (batchingConfiguration.isSuperBatchingEnabled() && batchingConfiguration.getSuperBatchModuleCompleteKeys().contains(moduleKey))
        {
            log.debug("Not requiring resource: " + moduleKey + " because it is part of a super-batch");
        }
        else
        {
            if (stack.contains(moduleKey))
            {
                log.warn("Cyclic plugin resource dependency has been detected with: " + moduleKey + "\n" + "Stack trace: " + stack);
                return;
            }

            final ModuleDescriptor<?> moduleDescriptor = webResourceIntegration.getPluginAccessor().getEnabledPluginModule(moduleKey);
            if (!(moduleDescriptor instanceof WebResourceModuleDescriptor))
            {
                log.warn("Cannot find web resource module for: " + moduleKey);
                return;
            }

            final List<String> dependencies = ((WebResourceModuleDescriptor) moduleDescriptor).getDependencies();
            log.debug("About to add resource [" + moduleKey + "] and its dependencies: " + dependencies);

            stack.push(moduleKey);
            try
            {
                for (final String dependency : dependencies)
                {
                    if (!orderedResourceKeys.contains(dependency))
                    {
                        addResourceWithDependencies(dependency, orderedResourceKeys, stack);
                    }
                }
            }
            finally
            {
                stack.pop();
            }
            orderedResourceKeys.add(moduleKey);
        }
    }

    /**
     * This is the equivalent of of calling {@link #includeResources(Writer, UrlMode, WebResourceFilter)} with
     * {@link UrlMode.AUTO} and a {@link DefaultWebResourceFilter}.
     *
     * @see #includeResources(Writer, UrlMode, WebResourceFilter)
     */
    public void includeResources(final Writer writer)
    {
        includeResources(writer, UrlMode.AUTO);
    }

    /**
     * This is the equivalent of of calling {@link #includeResources(Writer, UrlMode, WebResourceFilter)} with
     * the given url mode and a {@link DefaultWebResourceFilter}.
     *
     * @see #includeResources(Writer, UrlMode, WebResourceFilter)
     */
    public void includeResources(final Writer writer, final UrlMode urlMode)
    {
        includeResources(writer, true, urlMode, DefaultWebResourceFilter.INSTANCE);
    }

    /**
     * Writes out the resource tags to the previously required resources called via requireResource methods for the
     * specified url mode and resource filter. Note that this method will clear the list of previously required resources.
     *
     * @param writer the writer to write the links to
     * @param urlMode the url mode to write resource url links in
     * @param webResourceFilter the resource filter to filter resources on
     * @since 2.4
     */
    public void includeResources(final Writer writer, final UrlMode urlMode, final WebResourceFilter webResourceFilter)
    {
        includeResources(writer, true, urlMode, webResourceFilter);
    }

    /**
     * This is the equivalent of calling {@link #getRequiredResources(UrlMode, WebResourceFilter)} with
     * {@link UrlMode.AUTO} and a {@link DefaultWebResourceFilter}.
     *
     * @see #getRequiredResources(UrlMode, WebResourceFilter)
     */
    public String getRequiredResources()
    {
        return getRequiredResources(UrlMode.AUTO);
    }

    /**
     * This is the equivalent of calling {@link #getRequiredResources(UrlMode, WebResourceFilter)} with the given url
     * mode and a {@link DefaultWebResourceFilter}.
     *
     * @see #getRequiredResources(UrlMode, WebResourceFilter)
     */
    public String getRequiredResources(final UrlMode urlMode)
    {
        return getRequiredResources(urlMode, DefaultWebResourceFilter.INSTANCE);
    }

    /**
     * Returns a String of the resources tags to the previously required resources called via requireResource methods
     * for the specified url mode and resource filter. Note that this method will NOT clear the list of previously
     * required resources.
     *
     * @param urlMode the url mode to write out the resource tags
     * @param webResourceFilter the web resource filter to filter resources on
     * @return a String of the resource tags
     * @since 2.4
     */
    public String getRequiredResources(final UrlMode urlMode, final WebResourceFilter webResourceFilter)
    {
        final StringWriter writer = new StringWriter();
        includeResources(writer, false, urlMode, webResourceFilter);
        return writer.toString();
    }

    private void includeResources(final Writer writer, final boolean clearResources, final UrlMode urlMode, final WebResourceFilter filter)
    {
        List<PluginResource> superBatchResources = getSuperBatchResources(filter);

        includeSuperBatchResources(writer, urlMode, superBatchResources,  filter);

        final LinkedHashSet<String> webResourceNames = getWebResourceNames();
        if ((webResourceNames == null) || webResourceNames.isEmpty())
        {
            log.debug("No resources required to write");
            return;
        }

        includeResources(writer, webResourceNames, urlMode, filter);

        if (clearResources)
        {
            log.debug("Clearing previously required web resources");
            webResourceNames.clear();
        }
    }

    private List<PluginResource> getSuperBatchResources(WebResourceFilter filter)
    {
        List<PluginResource> resources = new ArrayList<PluginResource>();

        List<String> superBatchModuleKeys = batchingConfiguration.getSuperBatchModuleCompleteKeys();

        for (WebResourceFormatter formatter : webResourceFormatters)
        {
            Set<Map<String, String>> alreadyIncluded = new HashSet<Map<String, String>>();
            for (String moduleKey : superBatchModuleKeys)
            {
                final ModuleDescriptor<?> moduleDescriptor = webResourceIntegration.getPluginAccessor().getEnabledPluginModule(moduleKey);
                if (moduleDescriptor instanceof WebResourceModuleDescriptor)
                {
                    for (PluginResource pluginResource : pluginResourceLocator.getPluginResources(moduleDescriptor.getCompleteKey()))
                    {
                        if (formatter.matches(pluginResource.getResourceName()) && filter.matches(pluginResource.getResourceName()))
                        {
                            Map<String, String> batchParamsMap = new HashMap<String, String>(PluginResourceLocator.BATCH_PARAMS.length);

                            for (String s : PluginResourceLocator.BATCH_PARAMS)
                            {
                                batchParamsMap.put(s, pluginResource.getParams().get(s));
                            }

                            if (!alreadyIncluded.contains(batchParamsMap))
                            {
                                resources.add(SuperBatchPluginResource.createBatchFor(pluginResource));
                                alreadyIncluded.add(batchParamsMap);
                            }
                        }
                    }
                }
            }
        }
        return resources;
    }

    private void includeSuperBatchResources(Writer writer, UrlMode urlMode, List<PluginResource> resources, WebResourceFilter filter)
    {
        final Map<WebResourceFilter, Writer> writersForTypes = new HashMap<WebResourceFilter, Writer>();
        for (final WebResourceFormatter formatter : webResourceFormatters)
        {
            writersForTypes.put(formatter, new StringWriter());
        }

        for (PluginResource resource : resources)
        {
            writeResourceTag(resource.getModuleCompleteKey(), writersForTypes, urlMode, filter, writer, resource);
        }
        // write tags out in the order of formatters i.e. css then js
        for (final WebResourceFormatter formatter : webResourceFormatters)
        {
            writeContentAndSwallowErrors(writersForTypes.get(formatter).toString(), writer);
        }
    }

    private void includeResources(final Writer writer, final LinkedHashSet<String> webResourceNames, final UrlMode urlMode, final WebResourceFilter filter)
    {
        final Map<WebResourceFilter, Writer> writersForTypes = new HashMap<WebResourceFilter, Writer>();
        for (final WebResourceFormatter formatter : webResourceFormatters)
        {
            writersForTypes.put(formatter, new StringWriter());
        }

        for (final String resourceName : webResourceNames)
        {
            writeResourceTag(resourceName, writersForTypes, urlMode, filter, writer, pluginResourceLocator.getPluginResources(resourceName));
        }
        // write tags out in the order of formatters i.e. css then js
        for (final WebResourceFormatter formatter : webResourceFormatters)
        {
            writeContentAndSwallowErrors(writersForTypes.get(formatter).toString(), writer);
        }
    }

    public void requireResource(final String moduleCompleteKey, final Writer writer)
    {
        requireResource(moduleCompleteKey, writer, UrlMode.AUTO);
    }

    public void requireResource(final String moduleCompleteKey, final Writer writer, final UrlMode urlMode)
    {
        final LinkedHashSet<String> resourcesWithDeps = new LinkedHashSet<String>();
        addResourceWithDependencies(moduleCompleteKey, resourcesWithDeps, new Stack<String>());
        includeResources(writer, resourcesWithDeps, urlMode, DefaultWebResourceFilter.INSTANCE);
    }

    private void writeResourceTag(final String moduleCompleteKey, final Map<WebResourceFilter, Writer> writers, final UrlMode urlMode, final WebResourceFilter filter, final Writer errorHandler, List<PluginResource> resources)
    {
        if (resources == null)
        {
            writeContentAndSwallowErrors("<!-- Error loading resource \"" + moduleCompleteKey + "\".  Resource not found -->\n", errorHandler);
            return;
        }

        for (final PluginResource resource : resources)
        {
            writeResourceTag(moduleCompleteKey, writers, urlMode, filter, errorHandler, resource);
        }
    }

    private void writeResourceTag(String moduleCompleteKey, Map<WebResourceFilter, Writer> writers, UrlMode urlMode, WebResourceFilter filter, Writer errorHandler, PluginResource resource)
    {
        final String resourceName = resource.getResourceName();
        if (filter.matches(resourceName)) // only include resources that match the given filter
        {
            final WebResourceFormatter formatter = getWebResourceFormatter(resourceName);
            if (formatter == null)
            {
                writeContentAndSwallowErrors("<!-- Error loading resource \"" + moduleCompleteKey + "\".  Resource formatter not found -->\n",
                    errorHandler);
                return;
            }

            String url = resource.getUrl();
            if (resource.isCacheSupported())
            {
                url = getStaticResourcePrefix(resource.getVersion(webResourceIntegration), urlMode) + url;
            }
            else
            {
                url = webResourceIntegration.getBaseUrl(urlMode) + url;
            }
            writeContentAndSwallowErrors(formatter.formatResource(url, resource.getParams()), writers.get(formatter));
        }
        else if (log.isDebugEnabled())
        {
            log.debug("Resource [" + resourceName + "] excluded by filter.");
        }
    }

    public String getResourceTags(final String moduleCompleteKey)
    {
        return getResourceTags(moduleCompleteKey, UrlMode.AUTO);
    }

    public String getResourceTags(final String moduleCompleteKey, final UrlMode urlMode)
    {
        final StringWriter writer = new StringWriter();
        requireResource(moduleCompleteKey, writer, urlMode);
        return writer.toString();
    }

    private void writeContentAndSwallowErrors(final String content, final Writer writer)
    {
        try
        {
            writer.write(content);
        }
        catch (final IOException e)
        {
            log.debug(e);
        }
    }

    private WebResourceFormatter getWebResourceFormatter(final String resourceName)
    {
        for (final WebResourceFormatter webResourceFormatter : webResourceFormatters)
        {
            if (webResourceFormatter.matches(resourceName))
            {
                return webResourceFormatter;
            }
        }
        return null;
    }

    public String getStaticResourcePrefix()
    {
        return getStaticResourcePrefix(UrlMode.AUTO);
    }

    public String getStaticResourcePrefix(final UrlMode urlMode)
    {
        // "{base url}/s/{build num}/{system counter}/_"
        return webResourceIntegration.getBaseUrl(urlMode) + "/" + STATIC_RESOURCE_PREFIX + "/" + webResourceIntegration.getSystemBuildNumber() + "/" + webResourceIntegration.getSystemCounter() + "/" + STATIC_RESOURCE_SUFFIX;
    }

    public String getStaticResourcePrefix(final String resourceCounter)
    {
        return getStaticResourcePrefix(resourceCounter, UrlMode.AUTO);
    }

    public String getStaticResourcePrefix(final String resourceCounter, final UrlMode urlMode)
    {
        // "{base url}/s/{build num}/{system counter}/{resource counter}/_"
        return webResourceIntegration.getBaseUrl(urlMode) + "/" + STATIC_RESOURCE_PREFIX + "/" + webResourceIntegration.getSystemBuildNumber() + "/" + webResourceIntegration.getSystemCounter() + "/" + resourceCounter + "/" + STATIC_RESOURCE_SUFFIX;
    }

    public String getStaticPluginResource(final String moduleCompleteKey, final String resourceName)
    {
        return getStaticPluginResource(moduleCompleteKey, resourceName, UrlMode.AUTO);
    }

    public String getStaticPluginResource(final String moduleCompleteKey, final String resourceName, final UrlMode urlMode)
    {
        final ModuleDescriptor<?> moduleDescriptor = webResourceIntegration.getPluginAccessor().getEnabledPluginModule(moduleCompleteKey);
        if (moduleDescriptor == null)
        {
            return null;
        }

        return getStaticPluginResource(moduleDescriptor, resourceName, urlMode);
    }

    /**
     * @return "{base url}/s/{build num}/{system counter}/{plugin version}/_/download/resources/{plugin.key:module.key}/{resource.name}"
     */
    @SuppressWarnings("unchecked")
    public String getStaticPluginResource(final ModuleDescriptor moduleDescriptor, final String resourceName)
    {
        return getStaticPluginResource(moduleDescriptor, resourceName, UrlMode.AUTO);
    }

    public String getStaticPluginResource(final ModuleDescriptor moduleDescriptor, final String resourceName, final UrlMode urlMode)
    {
        // "{base url}/s/{build num}/{system counter}/{plugin version}/_"
        final String staticUrlPrefix = getStaticResourcePrefix(String.valueOf(moduleDescriptor.getPlugin().getPluginsVersion()), urlMode);
        // "/download/resources/plugin.key:module.key/resource.name"
        return staticUrlPrefix + pluginResourceLocator.getResourceUrl(moduleDescriptor.getCompleteKey(), resourceName);
    }

    /* Deprecated methods */

    /**
     * @deprecated Use {@link #getStaticPluginResource(com.atlassian.plugin.ModuleDescriptor, String)} instead
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public String getStaticPluginResourcePrefix(final ModuleDescriptor moduleDescriptor, final String resourceName)
    {
        return getStaticPluginResource(moduleDescriptor, resourceName);
    }

    /**
     * @deprecated Since 2.2
     */
    @Deprecated
    private static final String REQUEST_CACHE_MODE_KEY = "plugin.webresource.mode";

    /**
     * @deprecated Since 2.2
     */
    @Deprecated
    private static final IncludeMode DEFAULT_INCLUDE_MODE = WebResourceManager.DELAYED_INCLUDE_MODE;

    /**
     * @deprecated Since 2.2.
     */
    @Deprecated
    public void setIncludeMode(final IncludeMode includeMode)
    {
        webResourceIntegration.getRequestCache().put(REQUEST_CACHE_MODE_KEY, includeMode);
    }
}
