package com.atlassian.plugin.osgi.hostcomponents.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.*;

import com.atlassian.plugin.osgi.hostcomponents.InstanceBuilder;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;

/**
 * Default component registrar that also can write registered host components into the OSGi service registry
 */
public class DefaultComponentRegistrar implements ComponentRegistrar
{
    private List<Registration> registry = new ArrayList<Registration>();

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

        for (Registration reg : registry)
        {
            String[] names = new String[reg.getMainInterfaces().length];
            for (int x=0; x<names.length; x++)
                names[x] = reg.getMainInterfaces()[x].getName();

            reg.getProperties().put(HOST_COMPONENT_FLAG, Boolean.TRUE.toString());

            log.info("Registering: "+ Arrays.asList(names)+" instance "+reg.getInstance() + "with properties: "+reg.getProperties());
            ServiceRegistration sreg = ctx.registerService(names, reg.getInstance(), reg.getProperties());
            if (sreg != null)
                services.add(sreg);
        }
        return services;
    }

    List<Registration> getRegistry()
    {
        return registry;
    }
}
