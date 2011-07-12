package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.webresource.ContextBatchPluginResource.URL_PREFIX;

import com.atlassian.plugin.servlet.DownloadableResource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a fallback to serve resources relative to a context batch resource
 * In practice, the resources url should be transformed via the
 * {{com.atlassian.plugin.webresource.RelativeURLTransformResource}}.
 * This builder is in place in case this does not happen
 * @since 2.9.0
 */
public class ContextBatchSubResourceBuilder implements DownloadableResourceBuilder
{
    public static final Pattern CONTEXT_BATCH_PATTERN = Pattern.compile(URL_PREFIX + "\\w*/([^/]*)/(?!batch\\.js)(.*)$");

    private final ResourceDependencyResolver dependencyResolver;
    private final DownloadableResourceFinder resourceFinder;

    public ContextBatchSubResourceBuilder(final ResourceDependencyResolver dependencyResolver, final DownloadableResourceFinder resourceFinder)
    {
        this.dependencyResolver = dependencyResolver;
        this.resourceFinder = resourceFinder;
    }

    public boolean matches(final String path)
    {
        return CONTEXT_BATCH_PATTERN.matcher(path).find();
    }

    public DownloadableResource parse(final String path, final Map<String, String> params) throws UrlParseException
    {
        final Matcher m = CONTEXT_BATCH_PATTERN.matcher(path);

        if (!m.find())
        {
            throw new UrlParseException("Context batch url could not be parsed.");
        }

        final String type = ResourceUtils.getType(path);
        final String resourceName = m.group(2);
        final String key = m.group(1);

        for (final String context : getContexts(key))
        {
            for (final WebResourceModuleDescriptor moduleDescriptor : dependencyResolver.getDependenciesInContext(context))
            {
                final DownloadableResource resource = resourceFinder.find(moduleDescriptor.getCompleteKey(), resourceName);

                if (resource != null)
                {
                    return new BatchSubResource(resourceName, type, params, Arrays.asList(resource));
                }
            }
        }
        return new BatchSubResource(resourceName, type, params);
    }

    private List<String> getContexts(final String key)
    {
        return Arrays.asList(key.split(ContextBatchPluginResource.CONTEXT_SEPARATOR));
    }
}
