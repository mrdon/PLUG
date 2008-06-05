package com.atlassian.plugin.loaders.classloading.osgi.hostcomponents;

import org.osgi.framework.BundleContext;

public interface HostComponentProvider
{
    <T> InstanceBuilder<T> register(Class<T>... mainInterface);

    void writeRegistry(BundleContext ctx);
}
