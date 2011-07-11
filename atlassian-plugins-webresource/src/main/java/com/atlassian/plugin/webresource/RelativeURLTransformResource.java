package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.webresource.SinglePluginResource.URL_PREFIX;
import static com.atlassian.plugin.webresource.transformer.SearchAndReplacer.create;
import static com.atlassian.plugin.webresource.transformer.TransformerUtils.transformAndStreamResource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.util.concurrent.LazyReference;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Web resource that wraps an existing resource so that it can be transformed.
 * This wrapper converts relative urls in CSS resources into absolute urls.
 * 
 * @since 2.9.0
 */
final class RelativeURLTransformResource implements DownloadableResource
{
    static final Charset UTF8 = Charset.forName("UTF-8");
    static final Pattern CSS_URL_PATTERN = Pattern.compile("url\\s*\\(\\s*+([\"'])?+(?!/|https?://|data:)");

    static boolean matches(final ResourceDescriptor resource)
    {
        return CssWebResource.FORMATTER.matches(resource.getName());
    }

    static final Predicate<ResourceDescriptor> matcher = new Predicate<ResourceDescriptor>()
    {
        public boolean apply(final ResourceDescriptor input)
        {
            return matches(input);
        }
    };

    private final DownloadableResource originalResource;
    private final LazyReference<String> urlPrefix;

    RelativeURLTransformResource(final WebResourceUrlProvider webResourceUrlProvider, final ModuleDescriptor<?> moduleDescriptor, final DownloadableResource originalResource)
    {
        this.originalResource = originalResource;
        urlPrefix = new LazyReference<String>()
        {
            @Override
            protected String create()
            {
                final String version = moduleDescriptor.getPlugin().getPluginInformation().getVersion();
                return webResourceUrlProvider.getStaticResourcePrefix(version, UrlMode.RELATIVE) + URL_PREFIX + PATH_SEPARATOR + moduleDescriptor.getCompleteKey() + PATH_SEPARATOR;
            }
        };
    }

    CharSequence transform(final CharSequence originalContent)
    {
        final Function<Matcher, CharSequence> replacer = new Function<Matcher, CharSequence>()
        {
            public CharSequence apply(final Matcher matcher)
            {
                return new StringBuilder(matcher.group()).append(urlPrefix.get());
            }
        };
        return create(CSS_URL_PATTERN, replacer).replaceAll(originalContent);
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
        transformAndStreamResource(originalResource, UTF8, out, new Function<CharSequence, CharSequence>()
        {
            public CharSequence apply(final CharSequence originalContent)
            {
                return transform(originalContent);
            }
        });
    }

    public String getContentType()
    {
        return originalResource.getContentType();
    }
}
