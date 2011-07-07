package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.webresource.SinglePluginResource.URL_PREFIX;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.transformer.SearchAndReplacer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Function;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Web resource that wraps an existing resource so that it can be transformed.
 * This wrapper converts relative urls in CSS resources into absolute urls.
 * 
 * @since 2.10
 */
final class RelativeURLTransformResource implements DownloadableResource
{
    static final Charset UTF8 = Charset.forName("UTF-8");
    static final Pattern CSS_URL_PATTERN = Pattern.compile("url\\s*\\(\\s*+([\"'])?+(?!/|https?://|data:)");

    static boolean matches(final ResourceDescriptor resource)
    {
        return CssWebResource.FORMATTER.matches(resource.getName());
    }

    private final WebResourceUrlProvider webResourceUrlProvider;
    private final ModuleDescriptor<?> moduleDescriptor;
    private final DownloadableResource originalResource;

    RelativeURLTransformResource(final WebResourceUrlProvider WebResourceUrlProvider, final ModuleDescriptor<?> moduleDescriptor, final DownloadableResource originalResource)
    {
        webResourceUrlProvider = WebResourceUrlProvider;
        this.moduleDescriptor = moduleDescriptor;
        this.originalResource = originalResource;
    }

    String transform(final String originalContent)
    {
        final Function<Matcher, String> replacer = new Function<Matcher, String>()
        {
            final String urlPrefix = getUrlPrefix();

            public String apply(final Matcher matcher)
            {
                return matcher.group() + urlPrefix;
            }
        };
        return new SearchAndReplacer(CSS_URL_PATTERN, replacer).replaceAll(originalContent);
    }

    private String getUrlPrefix()
    {
        final String version = moduleDescriptor.getPlugin().getPluginInformation().getVersion();
        return webResourceUrlProvider.getStaticResourcePrefix(version, UrlMode.RELATIVE) + URL_PREFIX + PATH_SEPARATOR + moduleDescriptor.getCompleteKey() + PATH_SEPARATOR;
    }

    /*
     * Copied from AbstractTransformedDownloadableResource and AbstractStringTransformedDownloadableResource
     */
    public boolean isResourceModified(final HttpServletRequest request, final HttpServletResponse response)
    {
        return originalResource.isResourceModified(request, response);
    }

    public void serveResource(final HttpServletRequest request, final HttpServletResponse response) throws DownloadException
    {
        final String contentType = getContentType();
        if (StringUtils.isNotBlank(contentType))
        {
            response.setContentType(contentType);
        }

        try
        {
            streamResource(response.getOutputStream());
        }
        catch (final IOException e)
        {
            throw new DownloadException(e);
        }
    }

    public void streamResource(final OutputStream out) throws DownloadException
    {
        final ByteArrayOutputStream delegateOut = new ByteArrayOutputStream();
        originalResource.streamResource(delegateOut);

        try
        {
            final String originalContent = new String(delegateOut.toByteArray(), UTF8);
            IOUtils.copy(new StringReader(transform(originalContent)), out, UTF8.name());
        }
        catch (final IOException e)
        {
            throw new DownloadException("Unable to stream to the output", e);
        }
    }

    public String getContentType()
    {
        return originalResource.getContentType();
    }
}
