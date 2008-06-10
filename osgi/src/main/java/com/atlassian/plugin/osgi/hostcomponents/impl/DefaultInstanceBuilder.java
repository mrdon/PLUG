package com.atlassian.plugin.loaders.osgi.hostcomponents.impl;

import com.atlassian.plugin.loaders.osgi.hostcomponents.InstanceBuilder;
import com.atlassian.plugin.loaders.osgi.hostcomponents.PropertyBuilder;

class DefaultInstanceBuilder<T> implements InstanceBuilder<T>
{
    private Registration<T> registration;

    public DefaultInstanceBuilder(Registration<T> registration)
    {
        this.registration = registration;
    }

    public PropertyBuilder<T> forInstance(T instance)
    {
        registration.setInstance(instance);
        return new DefaultPropertyBuilder(registration);
    }
}
