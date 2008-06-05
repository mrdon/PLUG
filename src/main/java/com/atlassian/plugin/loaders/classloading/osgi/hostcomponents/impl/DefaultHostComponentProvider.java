package com.atlassian.plugin.loaders.classloading.osgi.hostcomponents.impl;

import com.atlassian.plugin.loaders.classloading.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.loaders.classloading.osgi.hostcomponents.InstanceBuilder;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import org.osgi.framework.BundleContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultHostComponentProvider implements HostComponentProvider
{
    private Map<Class<?>[], Registration<?>> registry = new HashMap();
    private Log log = LogFactory.getLog(DefaultHostComponentProvider.class);

    public DefaultHostComponentProvider()
    {
    }

    public <T> InstanceBuilder<T> register(Class<T>... mainInterface)
    {
        Registration<T> reg = new Registration(mainInterface);
        registry.put(mainInterface, reg);
        return new DefaultInstanceBuilder<T>(reg);
    }

    public void writeRegistry(BundleContext ctx)
    {
        for (Registration reg : registry.values())
        {
            String[] names = new String[reg.getMainInterface().length];
            for (int x=0; x<names.length; x++)
                names[x] = reg.getMainInterface()[x].getName();

            log.warn("Registering: "+ Arrays.asList(names)+" instance "+reg.getInstance() + "with properties: "+reg.getProperties());
            ctx.registerService(names, reg.getInstance(), reg.getProperties());
        }
    }

}
