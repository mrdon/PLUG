package com.atlassian.plugin.descriptors.servlet;

import com.atlassian.plugin.descriptors.servlet.util.DefaultPathMapper;
import com.atlassian.plugin.descriptors.servlet.util.PathMapper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A simple manager to track and retrieve the loaded servlet plugin modules.
 */
public class ServletModuleManager
{
    PathMapper mapper = new DefaultPathMapper();
    Map<String,ServletModuleDescriptor> descriptors = new HashMap<String,ServletModuleDescriptor>();
    Map<String,DelegatingPluginServlet> inittedServlets = new HashMap<String,DelegatingPluginServlet>();

    public DelegatingPluginServlet getServlet(String path, final ServletConfig servletConfig) throws ServletException
    {
        String completeKey = mapper.get(path);
        DelegatingPluginServlet servlet = null;

        if (completeKey != null)
        {
            servlet = inittedServlets.get(completeKey);

            if (servlet == null)
            {
                final ServletModuleDescriptor descriptor = descriptors.get(completeKey);

                if (descriptor != null)
                {
                    servlet = new DelegatingPluginServlet(descriptor);
                    servlet.init(new PluginServletConfig(descriptor, servletConfig));
                    inittedServlets.put(completeKey, servlet);
                }
            }
        }

        return servlet;
    }

    public void addModule(ServletModuleDescriptor descriptor)
    {
        descriptors.put(descriptor.getCompleteKey(), descriptor);

        for (String path : descriptor.getPaths())
            mapper.put(descriptor.getCompleteKey(), path);
    }

    public void removeModule(ServletModuleDescriptor descriptor)
    {
        descriptors.remove(descriptor.getCompleteKey());

        inittedServlets.remove(descriptor.getCompleteKey());

        mapper.put(descriptor.getCompleteKey(), null);
    }
}
