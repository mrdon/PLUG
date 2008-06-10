package com.atlassian.plugin.loaders.osgi.hostcomponents;

public interface InstanceBuilder<T>
{
    PropertyBuilder<T> forInstance(T instance);
}
