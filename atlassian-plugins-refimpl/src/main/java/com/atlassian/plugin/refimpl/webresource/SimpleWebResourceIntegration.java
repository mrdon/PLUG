package com.atlassian.plugin.refimpl.webresource;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.refimpl.ContainerManager;
import com.atlassian.plugin.refimpl.ParameterUtils;
import com.atlassian.plugin.webresource.WebResourceIntegration;

public class SimpleWebResourceIntegration implements WebResourceIntegration
{
    private final String systemBuildNumber;
    private final ThreadLocal<Map> requestCache;
    
    public SimpleWebResourceIntegration(ServletContext servletContext)
    {
        // we fake the build number by using the startup time which will force anything cached by clients to be 
        // reloaded after a restart
        this.systemBuildNumber = String.valueOf(System.currentTimeMillis());

        requestCache = new ThreadLocal<Map>();
    }

    public String getBaseUrl()
    {
        return ParameterUtils.getBaseUrl();
    }

    public PluginAccessor getPluginAccessor()
    {
        return ContainerManager.getInstance().getPluginAccessor();
    }

    public Map getRequestCache()
    {
        // if it's null, we just create a new one.. tho this means results from one request will affect the next request
        // on this same thread because we don't ever clean it up from a filter or anything - definitely not for use in
        // production!
        if (requestCache.get() == null)
            requestCache.set(new HashMap());
        
        return requestCache.get();
    }

    public String getSystemBuildNumber()
    {
        return systemBuildNumber;
    }

    public String getSystemCounter()
    {
        return "1";
    }

}
