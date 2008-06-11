package com.atlassian.plugin.osgi.hostcomponents.impl;

import com.atlassian.plugin.osgi.hostcomponents.PropertyBuilder;

/**
 * Default property builder for host components
 */
class DefaultPropertyBuilder implements PropertyBuilder
{
    private Registration registration;

    public DefaultPropertyBuilder(Registration registration)
    {
        this.registration = registration;
    }

    public PropertyBuilder withName(String name)
    {
        return withProperty("bean-name", name);
    }

    public PropertyBuilder withProperty(String name, String value)
    {
        registration.getProperties().put(name, value);
        return this;
    }
}
