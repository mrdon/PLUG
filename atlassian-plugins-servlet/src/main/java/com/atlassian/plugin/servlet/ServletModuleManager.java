package com.atlassian.plugin.servlet;

import com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.filter.FilterLocation;
import com.atlassian.plugin.servlet.filter.ServletFilterModuleContainerFilter;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * The ServletModuleManager is responsible for servlets and filters - and their servlet contexts - defined in plugins.
 * It is used by instances of the {@link ServletModuleContainerServlet} and {@link ServletFilterModuleContainerFilter}
 * to lookup, create and wrap the filters and servlets defined in plugins.  
 * <p/>
 * When the first {@link Filter} or {@link Servlet} is first accessed in a plugin, a new {@link ServletContext} is
 * created for all the modules in the plugin to share.  This is done by wrapping the applications 
 * {@link ServletContext}, creating a map of attributes that are local to the plugin that are shadowed by the 
 * applications {@link ServletContext} attributes, merging any servlet context init parameters from the plugin and the
 * application, and then running through any {@link ServletContextListener}s defined by the plugin has calling their
 * contextInitialized() methods. 
 * <p/>
 * The shadowing of the the plugins {@link ServletContext}s attributes are shadowed by the applications attributes 
 * means that if an attribute does not exist in the plugin local attribute map, the applications attributes will be 
 * returned.  The plugin is thereby prevented from modifying the base applications context attributes on an application
 * wide scope and can instead only change them, but not remove them, on a local scope.  
 * <p/>
 * The init parameters in the plugin will override parameters from the base applications servlet
 * init parameters that have the same name.
 * <p/>
 * During the creation of Filters and Servlets, the {@link FilterConfig} and {@link ServletConfig} provided to 
 * Filters and Servlets contain the plugin local {@link ServletContext}, as described above, 
 * and provides access to the init parameters defined in the plugin xml for the Filter or Servlet.   
 * <p/>
 * After being created, the filters and servlets are wrapped to ensure the the init(), service(), doFilter(), 
 * and destroy() methods and other methods defined in the Filter and Servlet interfaces are executed in the plugins
 * {@link ClassLoader}.
 * <p/>
 * The plugins {@link ServletContext} is not destroyed until the plugin is disabled.  It is also at this time that any
 * {@link ServletContextListener}s will have their contextDestroyed() methods called.
 */
public interface ServletModuleManager
{
    /**
     * Register a new servlet plugin module.
     * 
     * @param descriptor Details of what the servlet class is and the path it should serve.
     */
    void addServletModule(ServletModuleDescriptor<HttpServlet> descriptor);

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
    void removeServletModule(ServletModuleDescriptor<HttpServlet> descriptor);

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
    Iterable<Filter> getFilters(FilterLocation location, String pathInfo, FilterConfig filterConfig) throws ServletException;

    /**
     * Remove a previously registered filter plugin module.  Requests that come in on the path described in the 
     * descriptor will no longer be served.
     *  
     * @param descriptor Details of what filter module to remove.
     */
    void removeFilterModule(ServletFilterModuleDescriptor descriptor);
}
