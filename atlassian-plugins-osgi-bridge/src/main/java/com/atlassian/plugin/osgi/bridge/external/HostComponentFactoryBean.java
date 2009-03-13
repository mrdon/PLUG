package com.atlassian.plugin.osgi.bridge.external;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.osgi.context.BundleContextAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.InvalidSyntaxException;
import com.atlassian.plugin.PluginException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Simple factory bean to resolve host components.  Since we know host components won't change during the bundle's
 * lifetime, we can use a direct reference instead of the fancy proxy stuff from Spring DM.
 *
 * @since 2.2.0
 */
public class HostComponentFactoryBean implements FactoryBean, BundleContextAware
{
    private BundleContext bundleContext;
    private String filter;
    private Object service;

    public Object getObject() throws Exception
    {
        return findService();
    }

    public Class getObjectType()
    {
        return (findService() != null ? findService().getClass() : null);
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void setBundleContext(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
    }

    /**
     * Sets the OSGi service filter.
     *
     * @param filter OSGi filter describing the importing OSGi service
     */
    public void setFilter(String filter)
    {
        this.filter = filter;
    }

    /**
     * Finds a service, if the bundle context is available.
     *
     * @return The service, null if not found or the bundle context isn't available yet
     * @throws com.atlassian.plugin.PluginException
     *          If either 0 or more than 1 service reference is found
     */
    private Object findService() throws PluginException
    {
        if (service == null && bundleContext != null)
        {
            try
            {
                ServiceReference[] references = bundleContext.getServiceReferences(null, filter);
                if (references.length == 0)
                {
                    throw new PluginException("No service reference for '" + filter + "'");
                }
                if (references.length > 1)
                {
                    throw new PluginException("Too many service references found for '" + filter + "': " + Arrays.asList(references));
                }
                service = bundleContext.getService(references[0]);
            }
            catch (InvalidSyntaxException e)
            {
                throw new PluginException("Invalid filter syntax '" + filter + "'");
            }
        }
        return service;
    }
}
