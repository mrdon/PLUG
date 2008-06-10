package com.atlassian.plugin.osgi.hostcomponents;

import org.osgi.framework.BundleContext;

public interface HostComponentProvider
{

    static final String HOST_COMPONENT_FLAG = "plugins.host";
    
    <T> InstanceBuilder<T> register(Class<T>... mainInterface);

    void writeRegistry(BundleContext ctx);
}
