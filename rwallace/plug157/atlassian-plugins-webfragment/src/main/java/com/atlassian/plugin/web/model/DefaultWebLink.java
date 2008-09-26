package com.atlassian.plugin.web.model;

import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.descriptors.WebFragmentModuleDescriptor;
import org.dom4j.Element;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Represents a single "href", with a variety of permutations.
 */
public class DefaultWebLink extends AbstractWebItem implements WebLink
{
    String url;
    String accessKey;
    String id;

    public DefaultWebLink(Element linkEl, WebFragmentHelper webFragmentHelper, ContextProvider contextProvider, WebFragmentModuleDescriptor descriptor)
    {
        super(webFragmentHelper, contextProvider, descriptor);
        this.url = linkEl.getTextTrim();
        this.accessKey = linkEl.attributeValue("accessKey");
        this.id = linkEl.attributeValue("linkId");
    }

    public String getRenderedUrl(Map<String,Object> context)
    {
        context.putAll(getContextMap(context));
        return getWebFragmentHelper().renderVelocityFragment(url, context);
    }

    private boolean isRelativeUrl(String url)
    {
        return !(url.startsWith("http://") || url.startsWith("https://"));
    }

    public String getDisplayableUrl(HttpServletRequest req, Map<String,Object> context)
    {
        String renderedUrl = getRenderedUrl(context);
        if (isRelativeUrl(renderedUrl))
            return req.getContextPath() + renderedUrl;
        else
            return renderedUrl;
    }

    public boolean hasAccessKey()
    {
        return accessKey != null && !"".equals(accessKey);
    }

    public String getAccessKey(Map<String,Object> context)
    {
        context.putAll(getContextMap(context));
        return getWebFragmentHelper().renderVelocityFragment(accessKey, context);
    }

    public String getId()
    {
        return id;
    }
}
