package com.atlassian.plugin.webresource;

import com.atlassian.plugin.FileCacheService;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.util.PluginUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;

/**
 * Creates a batch of all like-typed resources that are declared as "super-batch="true"" in their plugin
 * definitions.
 * <p/>
 * The URL for batch resources is /download/superbatch/&lt;type>/batch.&lt;type. The additional type part in the path
 * is simply there to make the number of path-parts identical with other resources, so relative URLs will still work
 * in CSS files.
 */
public class SuperBatchPluginResource extends AbstractFileCacheResource implements DownloadableResource, BatchResource, PluginResource
{
    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "superbatch" + PATH_SEPARATOR;
    static final String DEFAULT_RESOURCE_NAME_PREFIX = "superbatch";

    private final BatchPluginResource delegate;
    private final String resourceName;
    private final String hash; //versions.. upgrades.. and such.. must handle that


    public static SuperBatchPluginResource createBatchFor(final PluginResource pluginResource, final String hash, final FileCacheService fileCacheService)
    {
        return new SuperBatchPluginResource(ResourceUtils.getType(pluginResource.getResourceName()), pluginResource.getParams(), hash, fileCacheService);
    }

    /**
     * Creates a super batch resource without the included resources
     *
     * @param type   the type of resource (CSS/JS)
     * @param params the parameters (ieOnly,media)
     */
    public SuperBatchPluginResource(final String type, final Map<String, String> params, final String hash, final FileCacheService fileCacheService)
    {
        this(type, params, Collections.<DownloadableResource>emptyList(), hash, fileCacheService);
    }

    public SuperBatchPluginResource(final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources, final String hash, final FileCacheService fileCacheService)
    {
        this(generateResourceName(type, hash), type, params, resources, hash, fileCacheService);
    }

    protected SuperBatchPluginResource(final String resourceName, final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources, final String hash, final FileCacheService fileCacheService)
    {
        super(fileCacheService);
        this.resourceName = resourceName;
        delegate = new BatchPluginResource(null, type, params, resources);
        this.hash = ammendHashWithLocale(hash);
    }

    public boolean isResourceModified(final HttpServletRequest request, final HttpServletResponse response)
    {
        return delegate.isResourceModified(request, response);
    }

    public void serveResource(final HttpServletRequest request, final HttpServletResponse response) throws DownloadException
    {
        if (isFileCacheEnabled())
        {
            try
            {
                final OutputStream out = response.getOutputStream();
                final Enumeration<String> na = request.getParameterNames();
                if (na.hasMoreElements())
                {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(hash).append(delegate.getType());

                    while (na.hasMoreElements())
                    {
                        final String name = na.nextElement();
                        sb.append(name).append(request.getParameter(name));
                    }
                    streamResource(getStream(hash, delegate.getType(), delegate), out);
                }
                else
                {
                    streamResource(out);
                }
            }
            catch (final IOException e)
            {
                throw new DownloadException(e);
            }
        }
        else
        {
            delegate.serveResource(request, response);
        }
    }

    public void streamResource(final OutputStream out) throws DownloadException
    {
        if (isFileCacheEnabled())
        {
            streamResource(getStream(hash, delegate.getType(), delegate), out);
        }
        else
        {
            delegate.streamResource(out);
        }
    }

    public String getContentType()
    {

        return delegate.getContentType();
    }

    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public String getUrl()
    {
        final StringBuilder buf = new StringBuilder(URL_PREFIX.length() + 20);
        buf.append(URL_PREFIX).append(getType()).append(PATH_SEPARATOR).append(resourceName);
        delegate.addParamsToUrl(buf, delegate.getParams());
        return buf.toString();
    }

    public Map<String, String> getParams()
    {
        return delegate.getParams();
    }

    public String getVersion(final WebResourceIntegration integration)
    {
        return integration.getSuperBatchVersion();
    }

    public String getType()
    {
        return delegate.getType();
    }

    public boolean isCacheSupported()
    {
        return true;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public String getModuleCompleteKey()
    {
        return "superbatch";
    }

    @Override
    public String toString()
    {
        return "[Superbatch name=" + resourceName + ", type=" + getType() + ", params=" + getParams() + "]";
    }

    private static String generateResourceName(final String type, final String hash)
    {
        if (Boolean.getBoolean(PluginUtils.ATLASSIAN_DEV_MODE))
        {
            return DEFAULT_RESOURCE_NAME_PREFIX + "." + type;
        }
        else
        {
            return DEFAULT_RESOURCE_NAME_PREFIX + "_" + hash + "." + type;
        }
    }

}
