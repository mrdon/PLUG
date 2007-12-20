package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.model.DefaultWebIcon;
import com.atlassian.plugin.web.model.DefaultWebLink;
import com.atlassian.plugin.web.model.WebIcon;
import com.atlassian.plugin.web.model.WebLink;
import com.atlassian.plugin.web.WebInterfaceManager;
import org.dom4j.Element;

/**
 * Represents a pluggable link.
 */
public class DefaultWebItemModuleDescriptor extends AbstractWebFragmentModuleDescriptor implements WebItemModuleDescriptor
{
    private String section;
    private WebIcon icon;
    private DefaultWebLink link;
    private String styleName;

    public DefaultWebItemModuleDescriptor(WebInterfaceManager webInterfaceManager)
    {
        super(webInterfaceManager);
    }

    public DefaultWebItemModuleDescriptor()
    {
    }

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);

        section = element.attributeValue("section");

        if (element.element("icon") != null)
        {
            icon = new DefaultWebIcon(element.element("icon"), webInterfaceManager.getWebFragmentHelper(), contextProvider, this);
        }

        if (element.element("styleName") != null)
        {
            styleName = element.element("styleName").getTextTrim();
        }
        else
        {
            styleName = "";
        }

        link = new DefaultWebLink(element.element("link"), webInterfaceManager.getWebFragmentHelper(), contextProvider, this);
    }

    public String getSection()
    {
        return section;
    }

    public WebLink getLink()
    {
        return link;
    }

    public WebIcon getIcon()
    {
        return icon;
    }

    public String getStyleName()
    {
        return styleName;
    }
}
