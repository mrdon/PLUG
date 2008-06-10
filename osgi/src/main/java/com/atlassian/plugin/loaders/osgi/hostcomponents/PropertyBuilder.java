package com.atlassian.plugin.loaders.osgi.hostcomponents;

public interface PropertyBuilder<T>
{
    PropertyBuilder<T> withName(String name);
    PropertyBuilder<T> withProperty(String name, String value);
}
