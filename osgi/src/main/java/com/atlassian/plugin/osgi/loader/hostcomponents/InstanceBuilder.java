package com.atlassian.plugin.osgi.loader.hostcomponents;

public interface InstanceBuilder<T>
{
    PropertyBuilder<T> forInstance(T instance);
}
