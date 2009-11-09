package com.atlassian.plugin.refimpl.webresource;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.atlassian.plugin.refimpl.ContainerManager;
import com.atlassian.plugin.webresource.WebResourceIntegration;

public class RequestCacheCleaner implements Filter
{
    private final WebResourceIntegration webResourceIntegration;
    
    public RequestCacheCleaner()
    {
        this.webResourceIntegration = ContainerManager.getInstance().getWebResourceIntegration();
    }
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        try
        {
            chain.doFilter(request, response);
        }
        finally
        {
            webResourceIntegration.getRequestCache().clear();
        }
    }

    public void init(FilterConfig arg0) throws ServletException {}
    public void destroy() {}
}
