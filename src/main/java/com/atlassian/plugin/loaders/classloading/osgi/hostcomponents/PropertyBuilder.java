package com.atlassian.plugin.loaders.classloading.osgi.hostcomponents;

public interface PropertyBuilder<T>
{
    PropertyBuilder<T> withName(String name);
    PropertyBuilder<T> withProperty(String name, String value);
}
