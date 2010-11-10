package com.atlassian.plugin.spring;

import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.servlet.DefaultServletModuleManager;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.util.ServletContextServletModuleManagerAccessor;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;

/**
 * A {@link ServletModuleManager} that has a {@link ServletContext} automatically injected
 */
public class SpringServletModuleManager extends DefaultServletModuleManager implements ServletContextAware
{
    // ------------------------------------------------------------------------------------------------------- Constants
    // ------------------------------------------------------------------------------------------------- Type Properties
    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors
    public SpringServletModuleManager(final PluginEventManager pluginEventManager)
    {
        super(pluginEventManager);
    }

    // ----------------------------------------------------------------------------------------------- Interface Methods
    // -------------------------------------------------------------------------------------------------- Public Methods
    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators
    public void setServletContext(final ServletContext servletContext)
    {
        ServletContextServletModuleManagerAccessor.setServletModuleManager(servletContext, this);
    }
}
