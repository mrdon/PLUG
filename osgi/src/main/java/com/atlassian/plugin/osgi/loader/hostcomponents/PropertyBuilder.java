package com.atlassian.plugin.osgi.loader.hostcomponents;

public interface PropertyBuilder<T>
{
    PropertyBuilder<T> withName(String name);
    PropertyBuilder<T> withProperty(String name, String value);
}
