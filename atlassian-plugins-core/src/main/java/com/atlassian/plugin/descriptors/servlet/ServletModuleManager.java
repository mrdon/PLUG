package com.atlassian.plugin.descriptors.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.util.*;

import com.atlassian.seraph.util.PathMapper;

/**
 * A simple manager to track and retrieve the loaded servlet plugin modules.
 */
public class ServletModuleManager
{
    PathMapper mapper = new PathMapper();
    Map descriptors = new HashMap();
    Map inittedServlets = new HashMap();

    public HttpServlet getServlet(String path, final ServletConfig servletConfig) throws ServletException
    {
        String completeKey = mapper.get(path);
        HttpServlet servlet = null;

        if (completeKey != null)
        {
            servlet = (HttpServlet) inittedServlets.get(completeKey);

            if (servlet == null)
            {
                final ServletModuleDescriptor descriptor = (ServletModuleDescriptor) descriptors.get(completeKey);

                if (descriptor != null)
                {
                    servlet = new DelegatingPluginServlet(descriptor);
                    servlet.init(new PluginServetConfig(descriptor, servletConfig));
                    inittedServlets.put(completeKey, servlet);
                }
            }
        }

        return servlet;
    }

    public void addModule(ServletModuleDescriptor descriptor)
    {
        descriptors.put(descriptor.getCompleteKey(), descriptor);

        for (Iterator iterator = descriptor.getPaths().iterator(); iterator.hasNext();)
        {
            String path = (String) iterator.next();
            mapper.put(descriptor.getCompleteKey(), path);
        }
    }

    public void removeModule(ServletModuleDescriptor descriptor)
    {
        descriptors.remove(descriptor.getCompleteKey());

        inittedServlets.remove(descriptor.getCompleteKey());

        mapper.put(descriptor.getCompleteKey(), null);
    }
}
