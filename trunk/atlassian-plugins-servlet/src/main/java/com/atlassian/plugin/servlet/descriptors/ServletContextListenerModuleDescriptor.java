package com.atlassian.plugin.servlet.descriptors;

import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.Validate;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

/**
 * Provides a way for plugins to declare {@link ServletContextListener}s so they can be notified when the 
 * {@link javax.servlet.ServletContext} is created for the plugin.  Implementors need to extend this class and implement the
 * {#link autowireObject} method.
 *
 * @since 2.1.0
 */
public class ServletContextListenerModuleDescriptor extends AbstractModuleDescriptor<ServletContextListener>
{
    protected static final Log log = LogFactory.getLog(ServletContextListenerModuleDescriptor.class);

    protected final HostContainer hostContainer;

    /**
     * Creates a descriptor that uses a module factory to create instances
     *
     * @param hostContainer The module factory
     * @since 2.2.0
     */
    public ServletContextListenerModuleDescriptor(HostContainer hostContainer)
    {
        Validate.notNull(hostContainer);
        this.hostContainer = hostContainer;
    }

    @Override
    public ServletContextListener getModule()
    {
        ServletContextListener obj;
        // Give the plugin a go first
        if (plugin instanceof AutowireCapablePlugin)
            obj = ((AutowireCapablePlugin)plugin).autowire(getModuleClass());
        else
        {
            obj = hostContainer.create(getModuleClass());
        }
        return obj;
    }

}
