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
    private String styleClass;

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

        if (element.element("styleClass") != null)
        {
            styleClass = element.element("styleClass").getTextTrim();
        }
        else
        {
            styleClass = "";
        }
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

    public String getStyleClass()
    {
        return styleClass;
    }

    public void enabled()
    {
        super.enabled();

        // contextProvider is not available until the module is enabled because they may need to have dependencies injected
        if (element.element("icon") != null)
        {
            icon = new DefaultWebIcon(element.element("icon"), webInterfaceManager.getWebFragmentHelper(), contextProvider, this);
        }
        if (element.element("link") != null)
        {
            link = new DefaultWebLink(element.element("link"), webInterfaceManager.getWebFragmentHelper(), contextProvider, this);
        }
    }
}
