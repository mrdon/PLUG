package com.atlassian.plugin.servlet.descriptors;

import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

/**
 * Provides a way for plugins to declare {@link ServletContextListener}s so they can be notified when the 
 * {@link ServletContext} is created for the plugin.  Implementors need to extend this class and implement the
 * {#link autowireObject} method.
 */
public abstract class ServletContextListenerModuleDescriptor extends AbstractModuleDescriptor<ServletContextListener>
{
    protected static final Log log = LogFactory.getLog(ServletContextListenerModuleDescriptor.class);

    @Override
    public ServletContextListener getModule()
    {
        ServletContextListener obj = null;
        try
        {
            // Give the plugin a go first
            if (plugin instanceof AutowireCapablePlugin)
                obj = ((AutowireCapablePlugin)plugin).autowire(getModuleClass());
            else
            {
                obj = getModuleClass().newInstance();
                autowireObject(obj);
            }
        }
        catch (InstantiationException e)
        {
            log.error("Error instantiating: " + getModuleClass(), e);
        }
        catch (IllegalAccessException e)
        {
            log.error("Error accessing: " + getModuleClass(), e);
        }
        return obj;
    }

    /**
     * Autowire an object. Implement this in your IoC framework or simply do nothing.
     */
    protected abstract void autowireObject(Object obj);
}
