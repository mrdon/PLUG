package com.atlassian.plugin.servlet;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.atlassian.plugin.servlet.descriptors.BaseServletModuleDescriptor;

/**
 * A request wrapper for requests bound for servlets declared in plugins.  Does the necessary path
 * munging for requests so that they look like they are 
 */
public class PluginHttpRequestWrapper extends HttpServletRequestWrapper
{
    private final String basePath;

    public PluginHttpRequestWrapper(HttpServletRequest request, BaseServletModuleDescriptor<?> descriptor)
    {
        super(request);
        this.basePath = findBasePath(descriptor);
    }

    public String getServletPath()
    {
        String servletPath = super.getServletPath();
        if (basePath != null)
        {
            servletPath += basePath;
        }
        return servletPath;
    }

    public String getPathInfo()
    {
        String pathInfo = super.getPathInfo();
        if (pathInfo != null && basePath != null)
            return pathInfo.substring(basePath.length());
        return pathInfo;
    }
    
    /**
     * A <a href="http://bluxte.net/blog/2006-03/29-40-33.html">commenter</a> based on the 
     * <a href="http://java.sun.com/products/servlet/2.1/html/introduction.fm.html#1499">servlet mapping spec</a>
     *  defined the mapping processing as:
     *  
     * <ol>
     *   <li>A string beginning with a '/' character and ending with a '/*' postfix is used for path mapping.</li>
     *   <li>A string beginning with a'*.' prefix is used as an extension mapping.</li>
     *   <li>A string containing only the '/' character indicates the "default" servlet of the application. In this 
     *       case the servlet path is the request URI minus the context path and the path info is null.</li>
     *   <li>All other strings are used for exact matches only.</li>
     * </ol>
     * 
     * To find the base path we're really only interested in the first case.  Everything else will just get a null
     * base path.  So we'll iterate through the list of paths specified and for the ones that match (1) above, check if
     * the path info returned by the super class matches.  If it does, we return that base path, otherwise we move onto
     * the next one.  
     */
    private String findBasePath(BaseServletModuleDescriptor<?> descriptor)
    {
        String pathInfo = super.getPathInfo();
        if (pathInfo != null)
        {
            for (Iterator<String> pathIterator = descriptor.getPaths().iterator(); pathIterator.hasNext(); )
            {
                String basePath = pathIterator.next();
                if (isPathMapping(basePath) && pathInfo.startsWith(getMappingRootPath(basePath)))
                {
                    return getMappingRootPath(basePath);
                }
            }
        }
        return null;
    }
    
    private boolean isPathMapping(String path)
    {
        return path.startsWith("/") && path.endsWith("/*");
    }
    
    private String getMappingRootPath(String pathMapping)
    {
        return pathMapping.substring(0, pathMapping.length() - 2);
    }
}
