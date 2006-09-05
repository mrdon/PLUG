package com.atlassian.plugin.web.model;

import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.loaders.LoaderUtils;
import org.dom4j.Element;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Represents a single "href", with a variety of permutations.
 */
public class WebLink
{
    String url;
    String accessKey;
    String id;
    private Map params;
    private WebFragmentHelper webFragmentHelper;

    public WebLink(Element linkEl, WebFragmentHelper webFragmentHelper)
    {
        this.webFragmentHelper = webFragmentHelper;
        this.url = linkEl.getTextTrim();
        this.accessKey = linkEl.attributeValue("accessKey");
        this.id = linkEl.attributeValue("linkId");
        this.params = LoaderUtils.getParams(linkEl);
    }

    public String getRenderedUrl(Map context)
    {
        return webFragmentHelper.renderVelocityFragment(url, context);
    }

    public boolean isRelativeUrl()
    {
        return !(url.startsWith("http://") || url.startsWith("https://"));
    }

    public String getDisplayableUrl(HttpServletRequest req, Map context)
    {
        if (isRelativeUrl())
            return req.getContextPath() + getRenderedUrl(context);
        else
            return getRenderedUrl(context);
    }

    public boolean hasAccessKey()
    {
        return accessKey != null && !"".equals(accessKey);
    }

    public String getAccessKey(Map context)
    {
        return webFragmentHelper.renderVelocityFragment(accessKey, context);
    }

    public String getId()
    {
        return id;
    }

    public Map getParams()
    {
        return params;
    }
}
