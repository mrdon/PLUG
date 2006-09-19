package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.model.WebIcon;
import com.atlassian.plugin.web.model.WebLink;
import com.atlassian.plugin.web.WebInterfaceManager;
import org.dom4j.Element;

public class WebItemModuleDescriptor extends AbstractWebFragmentModuleDescriptor
{
    private String section;
    private WebIcon icon;
    private WebLink link;

    public WebItemModuleDescriptor(WebInterfaceManager webInterfaceManager)
    {
        super(webInterfaceManager);
    }

    public WebItemModuleDescriptor()
    {
    }

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);

        section = element.attributeValue("section");

        if (element.element("icon") != null)
            icon = new WebIcon(element.element("icon"), webInterfaceManager.getWebFragmentHelper(), contextProvider);

        link = new WebLink(element.element("link"), webInterfaceManager.getWebFragmentHelper(), contextProvider);
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
}
