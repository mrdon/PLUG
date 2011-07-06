package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.transformer.SearchAndReplacer;
import com.google.common.base.Function;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.webresource.SinglePluginResource.URL_PREFIX;

/**
 * Web resource that wraps an existing resource so that it can be transformed.
 * This wrapper converts relative urls in CSS resources into absolute urls.
 */
public class RelativeURLTransformResource implements DownloadableResource
{
    public static final String CSS_URL_PATTERN = "url\\s*\\(\\s*+([\"'])?+(?!/|https?://|data:)";

    private final WebResourceUrlProvider webResourceUrlProvider;
    private final ModuleDescriptor moduleDescriptor;
    private final DownloadableResource originalResource;

    public RelativeURLTransformResource(WebResourceUrlProvider WebResourceUrlProvider, ModuleDescriptor moduleDescriptor, DownloadableResource originalResource)
    {
        this.webResourceUrlProvider = WebResourceUrlProvider;
        this.moduleDescriptor = moduleDescriptor;
        this.originalResource = originalResource;
    }

    public static boolean matches(ResourceDescriptor resource)
    {
        return CssWebResource.FORMATTER.matches(resource.getName());
    }

    protected String transform(String originalContent)
    {
        final String urlPrefix = getUrlPrefix();
        Function<Matcher, String> replacer = new Function<Matcher, String>() {
            public String apply(Matcher matcher) {
                return doReplace(urlPrefix, matcher);
            }
        };
        Pattern p = Pattern.compile(CSS_URL_PATTERN);
        SearchAndReplacer grep = new SearchAndReplacer(p, replacer);

        return grep.replaceAll(originalContent);
    }

    private String doReplace(String prefix, Matcher matcher) {
        return matcher.group() + prefix;
    }

    private String getUrlPrefix()
    {
        String version = moduleDescriptor.getPlugin().getPluginInformation().getVersion();
        return webResourceUrlProvider.getStaticResourcePrefix(version, UrlMode.RELATIVE) + URL_PREFIX +
                PATH_SEPARATOR + moduleDescriptor.getCompleteKey() + PATH_SEPARATOR;
    }

    /*
     * Copied from AbstractTransformedDownloadableResource and AbstractStringTransformedDownloadableResource
     */
    public boolean isResourceModified(HttpServletRequest request, HttpServletResponse response)
    {
        return originalResource.isResourceModified(request, response);
    }

    public void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException
    {
        final String contentType = getContentType();
        if (StringUtils.isNotBlank(contentType))
        {
            response.setContentType(contentType);
        }

        OutputStream out;
        try
        {
            out = response.getOutputStream();
        }
        catch (final IOException e)
        {
            throw new DownloadException(e);
        }

        streamResource(out);
    }

    public void streamResource(OutputStream out) throws DownloadException
    {
        ByteArrayOutputStream delegateOut = new ByteArrayOutputStream();
        originalResource.streamResource(delegateOut);

        try
        {
            String originalContent = new String(delegateOut.toByteArray(), getEncoding());
            String transformedContent = transform(originalContent);
            IOUtils.copy(new StringReader(transformedContent), out, getEncoding());
        }
        catch (UnsupportedEncodingException e)
        {
            // should never happen
            throw new DownloadException(e);
        }
        catch (IOException e)
        {
            throw new DownloadException("Unable to stream to the output", e);
        }
    }

    private String getEncoding()
    {
        return "UTF-8";
    }

    public String getContentType()
    {
        return originalResource.getContentType();
    }
}
