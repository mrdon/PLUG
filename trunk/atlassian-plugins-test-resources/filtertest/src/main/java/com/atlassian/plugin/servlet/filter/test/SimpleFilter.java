package com.atlassian.plugin.servlet.filter.test;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class SimpleFilter implements Filter
{
    String name;
    
    public void init(FilterConfig filterConfig) throws ServletException
    {
        name = filterConfig.getInitParameter("name");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        response.getWriter().write("entered: " + name + "\n");
        chain.doFilter(request, response);
        response.getWriter().write("exiting: " + name + "\n");
    }

    public void destroy() {}
}
