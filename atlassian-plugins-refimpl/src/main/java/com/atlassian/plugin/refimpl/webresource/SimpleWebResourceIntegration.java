package com.atlassian.plugin.refimpl.webresource;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.refimpl.ContainerManager;
import com.atlassian.plugin.refimpl.ParameterUtils;
import com.atlassian.plugin.webresource.WebResourceIntegration;
import com.atlassian.plugin.webresource.UrlMode;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

public class SimpleWebResourceIntegration implements WebResourceIntegration
{
    private final String systemBuildNumber;
    private final ThreadLocal<Map<String, Object>> requestCache = new ThreadLocal<Map<String,Object>>() {
        @Override
        protected Map<String, Object> initialValue()
        {
            // if it's null, we just create a new one.. tho this means results from one request will affect the next request
            // on this same thread because we don't ever clean it up from a filter or anything - definitely not for use in
            // production!
            return new HashMap<String, Object>();
        }
    };

    public SimpleWebResourceIntegration(final ServletContext servletContext)
    {
        // we fake the build number by using the startup time which will force anything cached by clients to be
        // reloaded after a restart
        systemBuildNumber = String.valueOf(System.currentTimeMillis());
    }

    public String getBaseUrl()
    {
        return getBaseUrl(UrlMode.AUTO);
    }

    public String getBaseUrl(UrlMode urlMode)
    {
        return ParameterUtils.getBaseUrl(urlMode);
    }

    public PluginAccessor getPluginAccessor()
    {
        return ContainerManager.getInstance().getPluginAccessor();
    }

    public Map<String, Object> getRequestCache()
    {
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
