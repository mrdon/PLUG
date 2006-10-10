package com.atlassian.plugin.web.model;

import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.descriptors.AbstractWebFragmentModuleDescriptor;
import org.dom4j.Element;

/**
 * Represents an icon associated with an item. It will not always be displayed!
 */
public class WebIcon
{
    private WebLink url;
    private int width;
    private int height;

    public WebIcon(Element iconEl, WebFragmentHelper webFragmentHelper, ContextProvider contextProvider, AbstractWebFragmentModuleDescriptor descriptor)
    {
        this.url = new WebLink(iconEl.element("link"), webFragmentHelper, contextProvider, descriptor);
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
