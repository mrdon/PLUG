package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.util.Either;
import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.plugin.util.collect.Function;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;

/**
 * Represents a batch of all resources that declare themselves as part of a given context(s).
 *
 * The URL for batch resources is /download/contextbatch/&lt;type>/&lt;contextname>/batch.&lt;type. The additional type part in the path
 * is simply there to make the number of path-parts identical with other resources, so relative URLs will still work
 * in CSS files.
 *
 * @since 2.9.0
 */
class ContextBatchPluginResource implements DownloadableResource, BatchResource, PluginResource
{
    static final String CONTEXT_SEPARATOR = ",";

    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "contextbatch" + PATH_SEPARATOR;

    private final BatchPluginResource delegate;
    private final String resourceName;
    private final String key;
    private final Iterable<String> contexts;
    private final String hash;
    private final String type;
    private final String filePath;
    private final Function<String, Either<DownloadException,InputStream>> transformer = new Function<String,  Either<DownloadException,InputStream>>()
    {
        public  Either<DownloadException,InputStream> get(final String input)
        {
            File f = new File(filePath, input + "." + type);
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

    ContextBatchPluginResource(final String key, final Iterable<String> contexts, final String hash, final String type, final Map<String, String> params, String filePath)
    {
        this(key, contexts, hash, type, params, Collections.<DownloadableResource>emptyList(),filePath);
    }

    ContextBatchPluginResource(final String key, final Iterable<String> contexts, final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources,String filePath)
    {
        this(key, contexts, null, type, params, resources, filePath);
    }

    public ContextBatchPluginResource(final String key, final Iterable<String> contexts, final String hash, final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources, String filePath)
    {
        if (Boolean.getBoolean(PluginUtils.ATLASSIAN_DEV_MODE))
        {
            resourceName = key + "." + type;
        }
        else
        {
            resourceName = key + "_" + hash + "." + type;
        }

        delegate = new BatchPluginResource(null, type, params, resources);
        this.key = key;
        this.contexts = contexts;
        this.type = type;
        this.filePath = filePath;
        this.hash = hash;
    }

    Iterable<String> getContexts()
    {
        return contexts;
    }

    public boolean isResourceModified(final HttpServletRequest request, final HttpServletResponse response)
    {
        return delegate.isResourceModified(request, response);
    }

    public void serveResource(final HttpServletRequest request, final HttpServletResponse response) throws DownloadException
    {
        if (Boolean.getBoolean(PluginUtils.ATLASSIAN_DEV_MODE))
        {
            delegate.serveResource(request, response);
        }
        else
        {
            try
            {
                OutputStream out = response.getOutputStream();
                streamResource(out);
            }
            catch(IOException e)
            {
              throw new DownloadException(e);
            }
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

    public String getContentType()
    {
        return delegate.getContentType();
    }

    boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public String getUrl()
    {
        final StringBuilder buf = new StringBuilder(URL_PREFIX.length() + 20);
        buf.append(URL_PREFIX).append(getType()).append(PATH_SEPARATOR).append(key).append(PATH_SEPARATOR).append(resourceName);
        delegate.addParamsToUrl(buf, delegate.getParams());
        return buf.toString();
    }

    public Map<String, String> getParams()
    {
        return delegate.getParams();
    }

    public String getVersion(final WebResourceIntegration integration)
    {
        return hash;
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
        return "contextbatch-" + resourceName;
    }

    @Override
    public String toString()
    {
        return "[Context Batch name=" + resourceName + ", type=" + getType() + ", params=" + getParams() + "]";
    }
}
