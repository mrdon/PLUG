package com.atlassian.plugin.servlet.descriptors;

import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.HostContainerLegacyAdaptor;
import com.atlassian.plugin.module.ModuleClassFactory;
import com.atlassian.plugin.servlet.ServletModuleManager;
import org.apache.commons.lang.Validate;

import javax.servlet.http.HttpServlet;

/**
 * A module descriptor that allows plugin developers to define servlets. Developers can define what urls the
 * servlet should be serve by defining one or more &lt;url-pattern&gt; elements.
 */
public class ServletModuleDescriptor extends BaseServletModuleDescriptor<HttpServlet> implements StateAware
{
    private final ServletModuleManager servletModuleManager;

    /**
     * Creates a descriptor that uses a module factory to create instances
     *
     * @param hostContainer the host container
     * @param servletModuleManager
     * @deprecated Since 2.5.0, use {@link #ServletModuleDescriptor(com.atlassian.plugin.module.ModuleClassFactory, com.atlassian.plugin.servlet.ServletModuleManager)} instead
     */
    public ServletModuleDescriptor(HostContainer hostContainer, ServletModuleManager servletModuleManager)
    {
        this(new HostContainerLegacyAdaptor(hostContainer), servletModuleManager);
    }

    /**
     * Creates a descriptor that uses a module factory to create instances
     *
     * @param moduleClassFactory
     * @since 2.2.0
     */
    public ServletModuleDescriptor(final ModuleClassFactory moduleClassFactory, final ServletModuleManager servletModuleManager)
    {
        super(moduleClassFactory);
        Validate.notNull(servletModuleManager);
        this.servletModuleManager = servletModuleManager;
    }

    @Override
    public void enabled()
    {
        super.enabled();
        servletModuleManager.addServletModule(this);
    }

    @Override
    public void disabled()
    {
        servletModuleManager.removeServletModule(this);
        super.disabled();
    }

    @Override
    public HttpServlet getModule()
    {
        return moduleClassFactory.createModuleClass(moduleClassName, this);
    }

    /**
     * @deprecated Since 2.0.0, use {@link #getModule}
     */
    @Deprecated
    public HttpServlet getServlet()
    {
        return getModule();
    }
}
