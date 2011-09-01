package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.util.Assertions.notNull;
import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Iterables.transform;

import com.atlassian.plugin.ModuleDescriptor;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    static final String STATIC_RESOURCE_PREFIX = "s";
    static final String STATIC_RESOURCE_SUFFIX = "_";

    static final String REQUEST_CACHE_RESOURCE_KEY = "plugin.webresource.names";
    static final String REQUEST_CACHE_CONTEXT_KEY = "plugin.webresource.contexts";

    protected final WebResourceIntegration webResourceIntegration;
    protected final PluginResourceLocator pluginResourceLocator;
    private final WebResourceUrlProvider webResourceUrlProvider;
    protected final ResourceBatchingConfiguration batchingConfiguration;
    protected final ResourceDependencyResolver dependencyResolver;
    protected static final List<WebResourceFormatter> webResourceFormatters = Arrays.asList(CssWebResource.FORMATTER, JavascriptWebResource.FORMATTER);

    private static final boolean IGNORE_SUPERBATCHING = false;

    public WebResourceManagerImpl(final PluginResourceLocator pluginResourceLocator, final WebResourceIntegration webResourceIntegration, final WebResourceUrlProvider webResourceUrlProvider)
    {
        this(pluginResourceLocator, webResourceIntegration, webResourceUrlProvider, new DefaultResourceBatchingConfiguration());
    }

    public WebResourceManagerImpl(final PluginResourceLocator pluginResourceLocator, final WebResourceIntegration webResourceIntegration, final WebResourceUrlProvider webResourceUrlProvider, final ResourceBatchingConfiguration batchingConfiguration)
    {
        this(pluginResourceLocator, webResourceIntegration, webResourceUrlProvider, batchingConfiguration, new DefaultResourceDependencyResolver(
            webResourceIntegration, batchingConfiguration));
    }

    public WebResourceManagerImpl(final PluginResourceLocator pluginResourceLocator, final WebResourceIntegration webResourceIntegration, final WebResourceUrlProvider webResourceUrlProvider, final ResourceBatchingConfiguration batchingConfiguration, final ResourceDependencyResolver dependencyResolver)
    {
        this.pluginResourceLocator = notNull("pluginResourceLocator", pluginResourceLocator);
        this.webResourceIntegration = notNull("webResourceIntegration", webResourceIntegration);
        this.webResourceUrlProvider = notNull("webResourceUrlProvider", webResourceUrlProvider);
        this.batchingConfiguration = notNull("batchingConfiguration", batchingConfiguration);
        this.dependencyResolver = notNull("dependencyResolver", dependencyResolver);
    }

    public void requireResource(final String moduleCompleteKey)
    {
        final boolean batchingEnabled = batchingConfiguration.isSuperBatchingEnabled();
        addAll(getIncludedResourceNames(), toModuleKeys(dependencyResolver.getDependencies(moduleCompleteKey, batchingEnabled)));
    }

    public void requireResourcesForContext(final String context)
    {
        getIncludedContexts().add(context);
    }

    public void includeResources(final Iterable<String> moduleCompleteKeys, final Writer writer, final UrlMode urlMode)
    {
        Iterable<String> resources = Lists.newArrayList();
        for (final String moduleCompleteKey : moduleCompleteKeys)
        {
            // Include resources from the super batch as we don't include the super batch itself
            final Iterable<String> dependencies = toModuleKeys(dependencyResolver.getDependencies(moduleCompleteKey, false));
            resources = concat(resources, dependencies);
        }

        // Resolve duplicates
        resources = ImmutableSet.copyOf(resources);
        writeResourceTags(getModuleResources(resources, Collections.<String> emptyList(), DefaultWebResourceFilter.INSTANCE), writer, urlMode);
    }

    /**
     * This is the equivalent of of calling {@link #includeResources(Writer, UrlMode, WebResourceFilter)} with
     * the given url mode and a {@link DefaultWebResourceFilter}.
     *
     * @see #includeResources(Writer, UrlMode, WebResourceFilter)
     */
    public void includeResources(final Writer writer, final UrlMode urlMode)
    {
        includeResources(writer, urlMode, DefaultWebResourceFilter.INSTANCE);
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
        writeIncludedResources(writer, urlMode, webResourceFilter);
        clear();
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
    public String getRequiredResources(final UrlMode urlMode, final WebResourceFilter filter)
    {
        return writeIncludedResources(new StringWriter(), urlMode, filter).toString();
    }

    /**
     * Write all currently included resources to the given writer.
     */
    private <W extends Writer> W writeIncludedResources(final W writer, final UrlMode urlMode, final WebResourceFilter filter)
    {
        final ContextBatchBuilder builder = new ContextBatchBuilder(pluginResourceLocator, dependencyResolver, batchingConfiguration);
        final Iterable<PluginResource> resourcesToInclude = concat(getSuperBatchResources(filter), builder.build(getIncludedContexts(), filter));
        for (final String skippedResource : builder.getSkippedResources())
        {
            requireResource(skippedResource);
        }
        final Iterable<PluginResource> moduleResources = getModuleResources(getIncludedResourceNames(), builder.getAllIncludedResources(), filter);
        return writeResourceTags(concat(resourcesToInclude, moduleResources), writer, urlMode);
    }

    /**
     * Get all super-batch resources that match the given filter. If superbatching is disabled this will just
     * return the empty list.
     *
     * Package private so it can be tested independently.
     */
    List<PluginResource> getSuperBatchResources(final WebResourceFilter filter)
    {
        if (!batchingConfiguration.isSuperBatchingEnabled())
        {
            return Collections.emptyList();
        }

        final Iterable<WebResourceModuleDescriptor> superBatchModuleKeys = dependencyResolver.getSuperBatchDependencies();
        final List<PluginResource> resources = new ArrayList<PluginResource>();

        // This is necessarily quite complicated. We need distinct superbatch resources for each combination of
        // resourceFormatter (i.e. separate CSS or JS resources), and also each unique combination of
        // BATCH_PARAMS (i.e. separate superbatches for print stylesheets, IE only stylesheets, and IE only print
        // stylesheets if they ever exist in the future)
        for (final WebResourceFormatter formatter : webResourceFormatters)
        {
            final Set<Map<String, String>> alreadyIncluded = new HashSet<Map<String, String>>();
            for (final WebResourceModuleDescriptor moduleDescriptor : superBatchModuleKeys)
            {
                for (final PluginResource pluginResource : pluginResourceLocator.getPluginResources(moduleDescriptor.getCompleteKey()))
                {
                    if (formatter.matches(pluginResource.getResourceName()) && filter.matches(pluginResource.getResourceName()))
                    {
                        final Map<String, String> batchParamsMap = new HashMap<String, String>(PluginResourceLocator.BATCH_PARAMS.length);
                        for (final String s : PluginResourceLocator.BATCH_PARAMS)
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
        return resources;
    }

    /**
     * Write the tags for the given set of resources to the writer. Writing will be done in order of
     * webResourceFormatters so that all CSS resources will be output before Javascript.
     */
    private <W extends Writer> W writeResourceTags(final Iterable<PluginResource> resourcesToInclude, final W writer, final UrlMode urlMode)
    {
        for (final WebResourceFormatter formatter : webResourceFormatters)
        {
            for (final Iterator<PluginResource> iter = resourcesToInclude.iterator(); iter.hasNext();)
            {
                final PluginResource resource = iter.next();
                if (formatter.matches(resource.getResourceName()))
                {
                    writeResourceTag(urlMode, resource, formatter, writer);
                    iter.remove();
                }
            }
        }

        for (final PluginResource resource : resourcesToInclude)
        {
            writeContentAndSwallowErrors(writer, "<!-- Error loading resource \"", resource.getModuleCompleteKey(),
                "\".  No resource formatter matches \"", resource.getResourceName(), "\" -->\n");
        }
        return writer;
    }

    private void writeResourceTag(final UrlMode urlMode, final PluginResource resource, final WebResourceFormatter formatter, final Writer writer)
    {
        final String prefix;
        if (resource.isCacheSupported())
        {
            prefix = webResourceUrlProvider.getStaticResourcePrefix(resource.getVersion(webResourceIntegration), urlMode);
        }
        else
        {
            prefix = webResourceUrlProvider.getBaseUrl(urlMode);
        }
        writeContentAndSwallowErrors(writer, formatter.formatResource(prefix + resource.getUrl(), resource.getParams()));
    }

    public void requireResource(final String moduleCompleteKey, final Writer writer, final UrlMode urlMode)
    {
        final Iterable<String> allDependentModuleKeys = toModuleKeys(dependencyResolver.getDependencies(moduleCompleteKey, IGNORE_SUPERBATCHING));
        final Iterable<String> empty = Collections.<String> emptyList();
        final Iterable<PluginResource> resourcesToInclude = getModuleResources(allDependentModuleKeys, empty, DefaultWebResourceFilter.INSTANCE);
        writeResourceTags(resourcesToInclude, writer, urlMode);
    }

    public String getResourceTags(final String moduleCompleteKey, final UrlMode urlMode)
    {
        final StringWriter writer = new StringWriter();
        requireResource(moduleCompleteKey, writer, urlMode);
        return writer.toString();
    }

    public String getStaticResourcePrefix(final String resourceCounter, final UrlMode urlMode)
    {
        return webResourceUrlProvider.getStaticResourcePrefix(resourceCounter, urlMode);
    }

    public String getStaticPluginResource(final String moduleCompleteKey, final String resourceName, final UrlMode urlMode)
    {
        return webResourceUrlProvider.getStaticPluginResourceUrl(moduleCompleteKey, resourceName, urlMode);
    }

    /**
     * @return "{base url}/s/{build num}/{system counter}/{plugin version}/_/download/resources/{plugin.key:module.key}/{resource.name}"
     */
    public String getStaticPluginResource(final ModuleDescriptor<?> moduleDescriptor, final String resourceName, final UrlMode urlMode)
    {
        return webResourceUrlProvider.getStaticPluginResourceUrl(moduleDescriptor, resourceName, urlMode);
    }

    public <T> T executeInNewContext(final Supplier<T> nestedExecution)
    {
        final Map<String, Object> cache = webResourceIntegration.getRequestCache();
        final Map<String, Object> storedState = copyOf(cache);

        // clear the cache, as the nestedExecution must be executed in an empty environment
        cache.clear();
        try
        {
            return nestedExecution.get();
        }
        finally
        {
            // restore state, regardless of what happened
            // we have to clear first to handle the following case:
            //    nestedExecution wrote to the cache with a key that isn't in storedState. In this case we don't want
            cache.clear();
            cache.putAll(storedState);
        }
    }

    //
    // private helpers
    //

    private Iterable<PluginResource> getModuleResources(final Iterable<String> webResourcePluginModuleKeys, final Iterable<String> batchedModules, final WebResourceFilter filter)
    {
        final List<PluginResource> includedResources = new LinkedList<PluginResource>(); // use linked list as it gets removed from when writing
        for (final String moduleKey : webResourcePluginModuleKeys)
        {
            if (contains(batchedModules, moduleKey))
            {
                // skip this resource if it is already in a batch
                continue;
            }

            final List<PluginResource> moduleResources = pluginResourceLocator.getPluginResources(moduleKey);
            for (final PluginResource moduleResource : moduleResources)
            {
                if (filter.matches(moduleResource.getResourceName()))
                {
                    includedResources.add(moduleResource);
                }
            }
        }
        return includedResources;
    }

    private void writeContentAndSwallowErrors(final Writer writer, final String... contents)
    {
        try
        {
            for (final String content : contents)
            {
                writer.write(content);
            }
        }
        catch (final IOException ignore)
        {}
    }

    private Set<String> getIncludedContexts()
    {
        return getOrCreateFromRequestCache(REQUEST_CACHE_CONTEXT_KEY);
    }

    private Set<String> getIncludedResourceNames()
    {
        return getOrCreateFromRequestCache(REQUEST_CACHE_RESOURCE_KEY);
    }

    private Set<String> getOrCreateFromRequestCache(final String key)
    {
        final Map<String, Object> cache = webResourceIntegration.getRequestCache();
        @SuppressWarnings("unchecked")
        Set<String> set = (Set<String>) cache.get(key);
        if (set == null)
        {
            set = Sets.newLinkedHashSet();
            cache.put(key, set);
        }
        return set;
    }

    private void clear()
    {
        getIncludedResourceNames().clear();
        getIncludedContexts().clear();
    }

    private Iterable<String> toModuleKeys(final Iterable<WebResourceModuleDescriptor> descriptors)
    {
        return transform(descriptors, new TransformDescriptorToKey());
    }

    //
    // deprecated impl to be removed in 3.0, only here for backwards compatibility not to be used
    //

    @Deprecated
    public String getRequiredResources()
    {
        return getRequiredResources(UrlMode.AUTO);
    }

    @Deprecated
    public String getResourceTags(final String moduleCompleteKey)
    {
        return getResourceTags(moduleCompleteKey, UrlMode.AUTO);
    }

    @Deprecated
    public String getStaticPluginResource(final ModuleDescriptor<?> moduleDescriptor, final String resourceName)
    {
        return getStaticPluginResource(moduleDescriptor, resourceName, UrlMode.AUTO);
    }

    @Deprecated
    public String getStaticPluginResource(final String moduleCompleteKey, final String resourceName)
    {
        return getStaticPluginResource(moduleCompleteKey, resourceName, UrlMode.AUTO);
    }

    @Deprecated
    public String getStaticPluginResourcePrefix(final ModuleDescriptor<?> moduleDescriptor, final String resourceName)
    {
        return getStaticPluginResource(moduleDescriptor, resourceName, UrlMode.AUTO);
    }

    @Deprecated
    public String getStaticResourcePrefix()
    {
        return webResourceUrlProvider.getStaticResourcePrefix(UrlMode.AUTO);
    }

    @Deprecated
    public String getStaticResourcePrefix(final UrlMode urlMode)
    {
        return webResourceUrlProvider.getStaticResourcePrefix(urlMode);
    }

    @Deprecated
    public String getStaticResourcePrefix(final String resourceCounter)
    {
        return getStaticResourcePrefix(resourceCounter, UrlMode.AUTO);
    }

    @Deprecated
    public void includeResources(final Writer writer)
    {
        includeResources(writer, UrlMode.AUTO);
    }

    @Deprecated
    public void requireResource(final String moduleCompleteKey, final Writer writer)
    {
        requireResource(moduleCompleteKey, writer, UrlMode.AUTO);
    }

    @Deprecated
    public void setIncludeMode(final IncludeMode includeMode)
    {
        webResourceIntegration.getRequestCache().put("plugin.webresource.mode", includeMode);
    }
}
