package com.atlassian.plugin.osgi.hostcomponents;

public interface InstanceBuilder<T>
{
    PropertyBuilder<T> forInstance(T instance);
}
