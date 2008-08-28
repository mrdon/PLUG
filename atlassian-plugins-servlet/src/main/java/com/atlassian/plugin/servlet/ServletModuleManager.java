package com.atlassian.plugin.servlet;

import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.filter.FilterLocation;

public interface ServletModuleManager
{
    /**
     * Register a new servlet plugin module.
     * 
     * @param descriptor Details of what the servlet class is and the path it should serve.
     */
    void addServletModule(ServletModuleDescriptor descriptor);

    /**
     * Return an instance of the HttpServlet that should be used to serve content matching the provided url path.
     * 
     * @param path Path of the incoming request to serve. 
     * @param servletConfig ServletConfig given to the delegating servlet. 
     * @return HttpServlet that has been registered to serve up content matching the passed in path.
     * @throws ServletException Thrown if there is a problem initializing the servlet to be returned.
     */
    HttpServlet getServlet(String path, final ServletConfig servletConfig) throws ServletException;

    /**
     * Remove a previously registered servlet plugin module.  Requests that come in on the path described in the 
     * descriptor will no longer be served.
     *  
     * @param descriptor Details of what servlet module to remove.
     */
    void removeServletModule(ServletModuleDescriptor descriptor);

    /**
     * Register a new filter plugin module.
     * 
     * @param descriptor Details of what the filter class is and the path it should serve.
     */
    void addFilterModule(ServletFilterModuleDescriptor descriptor);

    /**
     * Returns the filters that have been registered to filter requests at the specified path matching the location 
     * in the filter stack. 
     * 
     * @param location Place in the applications filter stack the filters should be applied.
     * @param pathInfo Path of the incoming request to filter.
     * @param filterConfig FilterConfig given to the delegating filter.
     * @return List of filters to be applied, already sorted by weight
     * @throws ServletException Thrown if there is a problem initializing one of the filters to apply.
     */
    List<Filter> getFilters(FilterLocation location, String pathInfo, FilterConfig filterConfig) throws ServletException;

    /**
     * Remove a previously registered filter plugin module.  Requests that come in on the path described in the 
     * descriptor will no longer be served.
     *  
     * @param descriptor Details of what filter module to remove.
     */
    void removeFilterModule(ServletFilterModuleDescriptor descriptor);
}
