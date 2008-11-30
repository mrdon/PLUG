package com.atlassian.plugin.servlet.descriptors;

import javax.servlet.http.HttpServlet;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.servlet.ServletModuleManager;

/**
 * A module descriptor that allows plugin developers to define servlets. Developers can define what urls the 
 * servlet should be serve by defining one or more &lt;url-pattern&gt; elements.
 */
public class ServletModuleDescriptor extends BaseServletModuleDescriptor<HttpServlet> implements StateAware
{
    private final ServletModuleManager servletModuleManager;
    private final HostContainer hostContainer;

    /**
     * @deprecated Since 2.2.0, don't extend and use {@link #ServletModuleDescriptor(com.atlassian.plugin.hostcontainer.HostContainer ,ServletModuleManager)} instead
     */
    @Deprecated
    public ServletModuleDescriptor()
    {
        this(null, null);
    }

    /**
     * Creates a descriptor that uses a module factory to create instances
     *
     * @param hostContainer The module factory
     * @since 2.2.0
     */
    public ServletModuleDescriptor(HostContainer hostContainer, ServletModuleManager servletModuleManager)
    {
        this.hostContainer = hostContainer;
        this.servletModuleManager = servletModuleManager;
    }

    public void enabled()
    {
        super.enabled();
        getServletModuleManager().addServletModule(this);
    }

    public void disabled()
    {
        getServletModuleManager().removeServletModule(this);
        super.disabled();
    }

    public HttpServlet getModule()
    {
        HttpServlet obj = null;
        try
        {
            // Give the plugin a go first
            if (plugin instanceof AutowireCapablePlugin)
                obj = ((AutowireCapablePlugin)plugin).autowire(getModuleClass());
            else
            {
                if (hostContainer != null)
                {
                    obj = hostContainer.create(getModuleClass());
                }
                else
                {
                    obj = getModuleClass().newInstance();
                    autowireObject(obj);
                }
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
     * @deprecated Since 2.0.0, use {@link #getModule}
     */
    public HttpServlet getServlet()
    {
        return getModule();
    }

    /**
     * @deprecated Since 2.2.0, don't extend and use a {@link com.atlassian.plugin.hostcontainer.HostContainer} instead
     */
    @Deprecated
    protected void autowireObject(Object obj)
    {
        throw new UnsupportedOperationException("This method must be overridden if a HostContainer is not used");
    }

    /**
     * @deprecated Since 2.2.0, don't extend and use a {@link com.atlassian.plugin.hostcontainer.HostContainer} instead
     */
    @Deprecated
    protected ServletModuleManager getServletModuleManager()
    {
        if (servletModuleManager == null)
        {
            throw new IllegalStateException("This method must be implemented if a HostContainer is not used");
        }

        return servletModuleManager;
    }
}
