package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.servlet.SelectiveSelfServingModuleDescriptor;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.servlet.AbstractDownloadableResource;
import com.atlassian.plugin.servlet.BaseFileServerServlet;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.elements.ResourceDescriptor;
import org.dom4j.Element;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * A way of linking to web 'resources', such as javascript or css.  This allows us to include resources once
 * on any given page, as well as ensuring that plugins can declare resources, even if they are included
 * at the bottom of a page.
 */
public class WebResourceModuleDescriptor extends AbstractModuleDescriptor implements SelectiveSelfServingModuleDescriptor
{
    private boolean footer;
    private boolean minified;
    private boolean combined;
    private static final String RESOURCE_TYPE_DOWNLOAD = "download";

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
        this.combined = "true".equalsIgnoreCase(element.attributeValue("combined"));
    }

    public boolean isMinified()
    {
        return minified;
    }

    public boolean isFooter()
    {
        return footer;
    }

    public boolean isCombined()
    {
        return combined;
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

    public DownloadableResource getDownloadableResource(BaseFileServerServlet servlet, String resourceName)
    {
        if (!isCombined())
        {
            return null;
        }
        String fileExt = getFileExtension(resourceName);
        if (StringUtils.isBlank(fileExt))
        {
            return null;
        }
        return combinedFiles(servlet, fileExt);
    }

    private DownloadableResource combinedFiles(BaseFileServerServlet servlet, String fileExt)
    {
        DownloadableResource downloadableResource = null;
        List /*<ResourceLocation>*/ locationList = new ArrayList();
        for (Iterator iterator1 = getResourceDescriptors().iterator(); iterator1.hasNext();)
        {
            ResourceDescriptor resourceDescriptor = (ResourceDescriptor) iterator1.next();
            String location = resourceDescriptor.getLocation();
            if (location.endsWith(fileExt))
            {
                ResourceLocation resourceLocation = getResourceLocation(RESOURCE_TYPE_DOWNLOAD, resourceDescriptor.getName());
                locationList.add(resourceLocation);
            }
        }
        if (!locationList.isEmpty())
        {
            downloadableResource = new CombinedDownloadableResource(servlet, getPlugin(), locationList);
        }
        return downloadableResource;
    }

    private String getFileExtension(String uri)
    {
        String ext = "";
        int lastDot = uri.lastIndexOf('.');
        if (lastDot != -1)
        {
            ext = uri.substring(lastDot);
        }
        return ext;
    }

    static class CombinedDownloadableResource extends AbstractDownloadableResource
    {
        private final List /*<ResourceLocation>*/  locationList;

        public CombinedDownloadableResource(BaseFileServerServlet servlet, Plugin plugin, List /*<ResourceLocation>*/ locationList)
        {
            super(servlet, plugin, (ResourceLocation) locationList.get(0), "");
            this.locationList = locationList;
        }

        public void serveResource(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
        {
            boolean setContent = false;
            try
            {
                for (Iterator iterator = locationList.iterator(); iterator.hasNext();)
                {
                    ResourceLocation resourceLocation = (ResourceLocation) iterator.next();
                    if (!setContent)
                    {
                        String contentType = getContentType();
                        httpServletResponse.setContentType(contentType);
                        setContent = true;
                    }
                    //
                    // now do a server side include of the resource into the output stream.
                    // This will combine the contents of the resources together
                    RequestDispatcher rd = httpServletRequest.getRequestDispatcher(resourceLocation.getLocation());
                    rd.include(httpServletRequest, httpServletResponse);
                }
            } catch (ServletException e)
            {
                throw new IOException(e.getMessage());
            }
        }

    }
}
