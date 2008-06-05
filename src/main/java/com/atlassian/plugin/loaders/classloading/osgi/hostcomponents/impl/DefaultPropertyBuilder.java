package com.atlassian.plugin.loaders.classloading.osgi.hostcomponents.impl;

import com.atlassian.plugin.loaders.classloading.osgi.hostcomponents.PropertyBuilder;

class DefaultPropertyBuilder<T> implements PropertyBuilder<T>
{
    private Registration<T> registration;

    public DefaultPropertyBuilder(Registration<T> registration)
    {
        this.registration = registration;
    }

    public PropertyBuilder<T> withName(String name)
    {
        return withProperty("bean-name", name);
    }

    public PropertyBuilder<T> withProperty(String name, String value)
    {
        registration.getProperties().put(name, value);
        return this;
    }
}
