package com.atlassian.plugin.loaders.classloading.osgi.hostcomponents;

public interface InstanceBuilder<T>
{
    PropertyBuilder<T> forInstance(T instance);
}
