package com.atlassian.plugin.osgi.loader.hostcomponents.impl;

import com.atlassian.plugin.osgi.loader.hostcomponents.InstanceBuilder;
import com.atlassian.plugin.osgi.loader.hostcomponents.PropertyBuilder;

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
