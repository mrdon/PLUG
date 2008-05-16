package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import org.dom4j.Element;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * A way of linking to web 'resources', such as javascript or css.  This allows us to include resources once
 * on any given page, as well as ensuring that plugins can declare resources, even if they are included
 * at the bottom of a page.
 */
public class WebResourceModuleDescriptor extends AbstractModuleDescriptor
{
    private boolean footer;
    private boolean minified;

    /**
     * As this descriptor just handles resources, you should never call this
     */
    public Object getModule()
    {
        throw new UnsupportedOperationException("There is no module for Web Resources");
    }

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        this.footer = "true".equalsIgnoreCase(element.attributeValue("footer"));
        this.minified = "true".equalsIgnoreCase(element.attributeValue("minified"));
    }

    public boolean isMinified()
    {
        return minified;
    }

    public boolean isFooter()
    {
        return footer;
    }

    /**
     * We override getResourceLocation so that we can switch the name of the resource file to use
     * if minification is in play.
     *
     * @param type the type of resource
     * @param name the name of the resource
     * @return possibly a modified ResourceLocation
     */
    public ResourceLocation getResourceLocation(final String type, final String name)
    {
        ResourceLocation baseLocation = super.getResourceLocation(type, name);
        //
        // This is a case of convention over configuration.  We allow the formatter to decide
        // on a suitable minified name (so far the xxxx-min.yyy convention) and serve up the
        // resources like this.  The assumption is that if you put minified="true" on the web
        // resource declaration, then you have xxxx-min.yyy files available to be served
        //
        boolean doMinification = this.isMinified();
        if (doMinification)
        {
            //
            // you are allowed to override the minification on a per resource basis (unlikely but possible)
            if ("false".equalsIgnoreCase(baseLocation.getParameter("minified")))
            {
                doMinification = false;
            }
        }
        if (doMinification)
        {
            WebResourceFormatter webResourceFormatter = WebResourceManagerImpl.getWebResourceFormatter(name);
            if (webResourceFormatter != null)
            {
                String minifiedLocation = webResourceFormatter.minifyResourceLink(baseLocation.getLocation());
                baseLocation = new ResourceLocation(minifiedLocation, baseLocation);
            }
        }
        return baseLocation;
    }

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
