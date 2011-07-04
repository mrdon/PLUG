package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.transformer.AbstractStringTransformedDownloadableResource;
import com.atlassian.plugin.webresource.transformer.SearchAndReplacer;
import com.google.common.base.Function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.webresource.SinglePluginResource.URL_PREFIX;

/**
 * Web resource transformer for converting relative urls in CSS resources into absolute urls.
 */
public class RelativeURLTransformResource extends AbstractStringTransformedDownloadableResource
{
    public static final String CSS_URL_PATTERN = "url\\s*\\(\\s*+(\"|')?+(?!http://)(?!/)(?!data:)";

    private final WebResourceIntegration webResourceIntegration;
    private final ModuleDescriptor moduleDescriptor;

    public RelativeURLTransformResource(WebResourceIntegration webResourceIntegration, ModuleDescriptor moduleDescriptor, DownloadableResource originalResource)
    {
        super(originalResource);
        this.webResourceIntegration = webResourceIntegration;
        this.moduleDescriptor = moduleDescriptor;
    }

    public static boolean matches(ResourceDescriptor resource)
    {
        return CssWebResource.FORMATTER.matches(resource.getName());
    }

    @Override
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
        String quote = "";
        if (matcher.group(1) != null)
        {
            quote = matcher.group(1);
        }

        return "url(" + quote + prefix;
    }

    private String getUrlPrefix()
    {
        return webResourceIntegration.getBaseUrl() + URL_PREFIX + PATH_SEPARATOR + moduleDescriptor.getCompleteKey() + PATH_SEPARATOR;
    }
}
