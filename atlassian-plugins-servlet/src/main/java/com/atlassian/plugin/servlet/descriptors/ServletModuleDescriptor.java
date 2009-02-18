package com.atlassian.plugin.servlet.descriptors;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.servlet.ServletModuleManager;

import javax.servlet.http.HttpServlet;

import org.apache.commons.lang.Validate;

/**
 * A module descriptor that allows plugin developers to define servlets. Developers can define what urls the
 * servlet should be serve by defining one or more &lt;url-pattern&gt; elements.
 */
public class ServletModuleDescriptor<T extends HttpServlet> extends BaseServletModuleDescriptor<T> implements StateAware
{
    private final ServletModuleManager servletModuleManager;
    private final HostContainer hostContainer;

    /**
     * Creates a descriptor that uses a module factory to create instances
     *
     * @param hostContainer The module factory
     * @since 2.2.0
     */
    public ServletModuleDescriptor(HostContainer hostContainer, ServletModuleManager servletModuleManager)
    {
        Validate.notNull(hostContainer);
        Validate.notNull(servletModuleManager);
        this.hostContainer = hostContainer;
        this.servletModuleManager = servletModuleManager;
    }

    public void enabled()
    {
        super.enabled();
        servletModuleManager.addServletModule(this);
    }

    public void disabled()
    {
        servletModuleManager.removeServletModule(this);
        super.disabled();
    }

    public T getModule()
    {
        T servlet;
        // Give the plugin a go first
        if (plugin instanceof AutowireCapablePlugin)
        {
            servlet = ((AutowireCapablePlugin) plugin).autowire(getModuleClass());
        }
        else
        {
            servlet = hostContainer.create(getModuleClass());
        }
        return servlet;
    }

    /**
     * @deprecated Since 2.0.0, use {@link #getModule}
     */
    public HttpServlet getServlet()
    {
        return getModule();
    }
}
