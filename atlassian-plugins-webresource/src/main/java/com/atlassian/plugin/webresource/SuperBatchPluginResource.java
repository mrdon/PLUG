package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;

import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.util.Either;
import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.plugin.util.collect.Function;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Creates a batch of all like-typed resources that are declared as "super-batch="true"" in their plugin
 * definitions.
 *
 * The URL for batch resources is /download/superbatch/&lt;type>/batch.&lt;type. The additional type part in the path
 * is simply there to make the number of path-parts identical with other resources, so relative URLs will still work
 * in CSS files.
 *
 */
public class SuperBatchPluginResource implements DownloadableResource, BatchResource, PluginResource
{
    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "superbatch" + PATH_SEPARATOR;
    static final String DEFAULT_RESOURCE_NAME_PREFIX = "sb";

    private final BatchPluginResource delegate;
    private final String resourceName;
    private final String hash;
    private final String filePath;

     private Function<String, Either<DownloadException,InputStream>> transformer = new Function<String, Either<DownloadException,InputStream>>()
    {
        public Either<DownloadException,InputStream> get(final String input)
        {
            File f = new File(filePath, input + "." + delegate.getType());
            try
            {
                if (!f.exists()) //not constructed yet
                {
                    FileOutputStream fout = new FileOutputStream(f);
                    delegate.streamResource(fout);
                    fout.flush();
                    fout.close();
                }
                return Either.right((InputStream)new FileInputStream(f));
            }
            catch (IOException e)
            {
                return Either.left(new DownloadException(e));
            }
            catch (DownloadException e)
            {
                return Either.left(new DownloadException(e));
            }
        }
    };

    public static SuperBatchPluginResource createBatchFor(final PluginResource pluginResource, String hash, String temp)
    {
        return new SuperBatchPluginResource(ResourceUtils.getType(pluginResource.getResourceName()), pluginResource.getParams(), hash,temp);
    }

    /**
     * Creates a super batch resource without the included resources
     * @param type the type of resource (CSS/JS)
     * @param params the parameters (ieOnly,media)
     */
    public SuperBatchPluginResource(final String type, final Map<String, String> params, String hash, String temp)
    {
        this(type, params, Collections.<DownloadableResource> emptyList(),hash,temp);
    }

    public SuperBatchPluginResource(final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources, String hash, String temp)
    {
        this(generateResourceName(type,hash), type, params, resources,hash,temp);
    }

    protected SuperBatchPluginResource(final String resourceName, final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources,String hash,String temp)
    {
        this.resourceName = resourceName;
        delegate = new BatchPluginResource(null, type, params, resources);
        this.hash = hash;
        this.filePath = temp;
    }

    public boolean isResourceModified(final HttpServletRequest request, final HttpServletResponse response)
    {
        return delegate.isResourceModified(request, response);
    }

    public void serveResource(final HttpServletRequest request, final HttpServletResponse response) throws DownloadException
    {
        if(Boolean.getBoolean(PluginUtils.ATLASSIAN_DEV_MODE))
        {
            delegate.serveResource(request,response);
            return;
        }
        try
        {
            OutputStream out = response.getOutputStream();
            Enumeration<String> na = request.getParameterNames();
            if(na.hasMoreElements())
            {
                StringBuilder sb = new StringBuilder();
                sb.append(hash);

                while(na.hasMoreElements())
                {
                    String name = na.nextElement();
                    sb.append(name).append(request.getParameter(name));
                }
                Either<DownloadException,InputStream> result =transformer.get(Integer.toString(sb.toString().hashCode()));
                if(result.isRight())
                {
                    streamResource(result.right(),out);
                }
                else
                {
                    throw result.left();
                }
            }else
            {
                streamResource(out);
            }
        }
        catch(IOException e)
        {
          throw new DownloadException(e);
        }
    }

    public void streamResource(final OutputStream out) throws DownloadException
    {
        if (Boolean.getBoolean(PluginUtils.ATLASSIAN_DEV_MODE))
        {
            delegate.streamResource(out);
        }
        else
        {
            Either<DownloadException,InputStream> result = transformer.get(hash);
            if(result.isRight())
            {
                streamResource(result.right(), out);
            }
            else
            {
                throw result.left();
            }
        }
    }

    public String getContentType()
    {

        return delegate.getContentType();
    }

    /**
     * Copy from the supplied OutputStream to the supplied InputStream. Note
     * that the InputStream will be closed on completion.
     *
     * @param in the stream to read from
     * @param out the stream to write to
     * @throws DownloadException if an IOException is encountered writing to the
     *             out stream
     */
    private void streamResource(final InputStream in, final OutputStream out) throws DownloadException
    {
        try
        {
            IOUtils.copy(in, out);
        }
        catch (final IOException e)
        {
            throw new DownloadException(e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
            try
            {
                out.flush();
            }
            catch (final IOException e)
            {
               throw new DownloadException(e);
            }
        }
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

    private static String generateResourceName(String type, String hash)
    {
        if (Boolean.getBoolean(PluginUtils.ATLASSIAN_DEV_MODE))
        {
           return DEFAULT_RESOURCE_NAME_PREFIX + "." + type;
        }
        else
        {
           return DEFAULT_RESOURCE_NAME_PREFIX+"_"+hash + "." + type;
        }
    }
    
}
