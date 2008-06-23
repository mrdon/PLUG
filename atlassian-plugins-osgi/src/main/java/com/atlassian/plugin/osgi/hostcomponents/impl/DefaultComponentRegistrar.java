package com.atlassian.plugin.osgi.hostcomponents.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.*;

import com.atlassian.plugin.osgi.hostcomponents.InstanceBuilder;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

/**
 * Default component registrar that also can write registered host components into the OSGi service registry
 */
public class DefaultComponentRegistrar implements ComponentRegistrar
{
    private List<HostComponentRegistration> registry = new ArrayList<HostComponentRegistration>();

    private Log log = LogFactory.getLog(DefaultComponentRegistrar.class);

    public InstanceBuilder register(Class<?>... mainInterfaces)
    {
        Registration reg = new Registration(mainInterfaces);
        registry.add(reg);
        return new DefaultInstanceBuilder(reg);
    }

    public List<ServiceRegistration> writeRegistry(BundleContext ctx)
    {
        ArrayList<ServiceRegistration> services = new ArrayList<ServiceRegistration>();

        for (HostComponentRegistration reg : registry)
        {
            String[] names = reg.getMainInterfaces();

            reg.getProperties().put(HOST_COMPONENT_FLAG, Boolean.TRUE.toString());

            log.info("Registering: "+ Arrays.asList(names)+" instance "+reg.getInstance() + "with properties: "+reg.getProperties());
            ServiceRegistration sreg = ctx.registerService(names, reg.getInstance(), reg.getProperties());
            if (sreg != null)
            {
                services.add(sreg);
            }
        }
        return services;
    }

    public List<HostComponentRegistration> getRegistry()
    {
        return registry;
    }
}
