package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.cache.filecache.FileCacheStreamProvider;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.any;

/**
 * Represents a batch of plugin resources. <p/>
 * <p/>
 * It provides methods to parse and generate urls to locate a batch of plugin resources. <p/>
 * <p/>
 * Note BatchPluginResource is also a type of {@link DownloadableResource}. The underlying implementation simply
 * keeps a list of {@link DownloadableResource} of which this batch represents and delegates method calls.
 *
 * @since 2.2
 */
public class BatchPluginResource implements DownloadableResource, CacheablePluginResource, BatchResource, FileCacheStreamProvider
{
    private static final Logger log = LoggerFactory.getLogger(BatchPluginResource.class);

    /**
     * The url prefix for a batch of plugin resources: "/download/batch/"
     */
    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "batch" +PATH_SEPARATOR;

    private final String type;
    private final String moduleCompleteKey;
    private final Map<String, String> params;
    private final String resourceName;
    private final Iterable<DownloadableResource> resources;

    /**
     * A constructor that creates a default resource name for the batch in the format: moduleCompleteKey.type
     * For example: test.plugin:resources.js
     * <p/>
     * Note that name of the batch does not identify what the batch includes and could have been static e.g. batch.js
     *
     * @param moduleCompleteKey - the key of the plugin module
     * @param type              - the type of resource (CSS/JS)
     * @param params            - the parameters of the resource (ieonly, media, etc)
     */
    public BatchPluginResource(final String moduleCompleteKey, final String type, final Map<String, String> params)
    {
        this(moduleCompleteKey + "." + type, moduleCompleteKey, type, params, Collections.<DownloadableResource>emptyList());
    }

    /**
     * A constructor that creates a default resource name for the batch in the format: moduleCompleteKey.type
     * For example: test.plugin:resources.js
     * <p/>
     * This constructor includes the resources that are contained in the batch, and so is primarily for use
     * when serving the resource.
     *
     * @param moduleCompleteKey - the key of the plugin module
     * @param type              - the type of resource (CSS/JS)
     * @param params            - the parameters of the resource (ieonly, media, etc)
     * @param resources         - the resources included in the batch.
     */
    public BatchPluginResource(final String moduleCompleteKey, final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources)
    {
        this(moduleCompleteKey + "." + type, moduleCompleteKey, type, params, resources);
    }

    /**
     * This constructor should only ever be used internally within this class. It does not ensure that the resourceName's
     * file extension is the same as the given type. It is up to the calling code to ensure this.
     *
     * @param resourceName      - the full name of the resource
     * @param moduleCompleteKey - the key of the plugin module
     * @param type              - the type of resource (CSS/JS)
     * @param params            - the parameters of the resource (ieonly, media, etc)
     * @param resources         - the resources included in the batch.
     */
    BatchPluginResource(final String resourceName, final String moduleCompleteKey, final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources)
    {
        this.resourceName = resourceName;
        this.moduleCompleteKey = moduleCompleteKey;
        this.type = type;
        this.params = ImmutableMap.copyOf(params);
        this.resources = copyOf(resources);
    }

    /**
     * @return true if there are no resources included in this batch
     */
    public boolean isEmpty()
    {
        return Iterables.isEmpty(resources);
    }

    public boolean isResourceModified(final HttpServletRequest request, final HttpServletResponse response)
    {
        return any(resources, new Predicate<DownloadableResource>()
        {
            public boolean apply(final DownloadableResource resource)
            {
                return resource.isResourceModified(request, response);
            }
        });
    }

    public void writeStream(OutputStream dest) throws DownloadException
    {
        streamResource(dest);
    }

    public void serveResource(final HttpServletRequest request, final HttpServletResponse response) throws DownloadException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Start to serve batch " + toString());
        }
        for (final DownloadableResource resource : resources)
        {
            resource.serveResource(request, response);
            writeNewLine(response);
        }
    }

    public void streamResource(final OutputStream out) throws DownloadException
    {
        for (final DownloadableResource resource : resources)
        {
            resource.streamResource(out);
            writeNewLine(out);
        }
    }

    public String getContentType()
    {
        final String contentType = params.get("content-type");
        if (contentType != null)
        {
            return contentType;
        }
        return null;
    }

    /**
     * Returns a url string in the format: /download/batch/MODULE_COMPLETE_KEY/resourceName?PARAMS
     * <p/>
     * e.g. /download/batch/example.plugin:webresources/example.plugin:webresources.css?ie=true
     * <p/>
     * It is important for the url structure to be:
     * 1. the same number of sectioned paths as the SinglePluginResource
     * 2. include the module completey key in the path before the resource name
     * This is due to css resources referencing other resources such as images in relative path forms.
     * @param integration WebResourceIntegration to allow the resource to have locale and version information in the url.
     */
    public String getCacheUrl(WebResourceIntegration integration)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(URL_PREFIX).append(integration.getStaticResourceLocale()).append(PATH_SEPARATOR).append(getVersion(integration)).append(PATH_SEPARATOR).append(moduleCompleteKey).append(PATH_SEPARATOR).append(resourceName);
        addParamsToUrl(sb, params);
        return sb.toString();
    }

    /**
     * Returns a url string in the format: /download/batch/MODULE_COMPLETE_KEY/resourceName?PARAMS
     * <p/>
     * e.g. /download/batch/example.plugin:webresources/example.plugin:webresources.css?ie=true
     * <p/>
     * It is important for the url structure to be:
     * 1. the same number of sectioned paths as the SinglePluginResource
     * 2. include the module completey key in the path before the resource name
     * This is due to css resources referencing other resources such as images in relative path forms.
     */
    public String getUrl()
    {
        final StringBuilder sb = new StringBuilder();
        //we need to include a 'locale' and a 'version' to make the parser significantly easier to maintain, so we put dummy values in here as we can not reasonably guess these values
        sb.append(URL_PREFIX).append("locale").append(PATH_SEPARATOR).append("0.0").append(PATH_SEPARATOR).append(moduleCompleteKey).append(PATH_SEPARATOR).append(resourceName);
        addParamsToUrl(sb, params);
        return sb.toString();
    }

    protected void addParamsToUrl(final StringBuilder sb, final Map<String, String> params)
    {
        if (params.size() > 0)
        {
            sb.append("?");
            int count = 0;

            for (final Map.Entry<String, String> entry : params.entrySet())
            {
                try
                {
                    sb.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));

                    if (++count < params.size())
                    {
                        sb.append("&");
                    }
                }
                catch (final UnsupportedEncodingException e)
                {
                    log.error("Could not encode parameter to url for [" + entry.getKey() + "] with value [" + entry.getValue() + "]", e);
                }
            }
        }
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public Map<String, String> getParams()
    {
        return params;
    }

    public String getVersion(final WebResourceIntegration integration)
    {
        final Plugin plugin = integration.getPluginAccessor().getEnabledPluginModule(getModuleCompleteKey()).getPlugin();
        return plugin.getPluginInformation().getVersion();
    }

    public String getModuleCompleteKey()
    {
        return moduleCompleteKey;
    }

    public boolean isCacheSupported()
    {
        return !"false".equals(params.get("cache"));
    }

    public String getType()
    {
        return type;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass()))
        {
            return false;
        }

        final BatchPluginResource that = (BatchPluginResource) o;

        if (moduleCompleteKey != null ? !moduleCompleteKey.equals(that.moduleCompleteKey) : that.moduleCompleteKey != null)
        {
            return false;
        }
        if (params != null ? !params.equals(that.params) : that.params != null)
        {
            return false;
        }
        if (type != null ? !type.equals(that.type) : that.type != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result;
        result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (moduleCompleteKey != null ? moduleCompleteKey.hashCode() : 0);
        result = 31 * result + (params != null ? params.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "[moduleCompleteKey=" + moduleCompleteKey + ", type=" + type + ", params=" + params + "]";
    }

    /**
     * If a minified files follows another file and the former does not have a free floating carriage return AND ends in
     * a // comment then the one line minified file will in fact be lost from view in a batched send.  So we need
     * to put a new line between files
     *
     * @param response the HTTP response
     * @throws com.atlassian.plugin.servlet.DownloadException
     *          wraps an IOException (probably client abort)
     */
    private static void writeNewLine(final HttpServletResponse response) throws DownloadException
    {
        try
        {
            writeNewLine(response.getOutputStream());
        }
        catch (final IOException e)
        {
            throw new DownloadException(e);
        }
    }

    private static void writeNewLine(final OutputStream out) throws DownloadException
    {
        try
        {
            out.write('\n');
        }
        catch (final IOException e)
        {
            throw new DownloadException(e);
        }
    }
}
