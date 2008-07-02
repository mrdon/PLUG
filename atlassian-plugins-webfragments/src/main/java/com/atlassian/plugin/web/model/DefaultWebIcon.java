package com.atlassian.plugin.web.model;

import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.descriptors.WebFragmentModuleDescriptor;
import org.dom4j.Element;

/**
 * Represents an icon associated with an item. It will not always be displayed!
 */
public class DefaultWebIcon implements WebIcon
{
    private WebLink url;
    private int width;
    private int height;

    public DefaultWebIcon(Element iconEl, WebFragmentHelper webFragmentHelper, ContextProvider contextProvider, WebFragmentModuleDescriptor descriptor)
    {
        this.url = new DefaultWebLink(iconEl.element("link"), webFragmentHelper, contextProvider, descriptor);
        this.width = Integer.parseInt(iconEl.attributeValue("width"));
        this.height = Integer.parseInt(iconEl.attributeValue("height"));
    }

    public WebLink getUrl()
    {
        return url;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }
}
