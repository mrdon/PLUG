package com.atlassian.plugin.osgi.hostcomponents.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.*;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

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

            if (log.isDebugEnabled())
                log.debug("Registering: "+ Arrays.asList(names)+" instance "+reg.getInstance() + "with properties: "+reg.getProperties());

            ServiceRegistration sreg = ctx.registerService(names, wrapService(reg.getMainInterfaceClasses(), reg.getInstance()), reg.getProperties());
            if (sreg != null)
            {
                services.add(sreg);
            }
        }
        return services;
    }

    /**
     * Wraps the service in a dynamic proxy that ensures all methods are executed with the object class's class loader
     * as the context class loader
     * @param interfaces The interfaces to proxy
     * @param service The instance to proxy
     * @return A proxy that wraps the service
     */
    protected Object wrapService(Class[] interfaces, final Object service)
    {
        final Object wrappedService = Proxy.newProxyInstance(
            getClass().getClassLoader(),
            interfaces,
            new InvocationHandler() {
                public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                    final Thread thread = Thread.currentThread();
                    final ClassLoader ccl = thread.getContextClassLoader();
                    try {
                        thread.setContextClassLoader(service.getClass().getClassLoader());
                        return method.invoke(service, objects);
                    } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                    } finally {
                        thread.setContextClassLoader(ccl);
                    }
                }
            }
        );
        return wrappedService;
    }

    public List<HostComponentRegistration> getRegistry()
    {
        return registry;
    }
}
