package com.atlassian.plugin.osgi.hostcomponents;

/**
 * Ties properties to the host component registration
 */
public interface PropertyBuilder
{
    /**
     * Sets the bean name of the host component
     * @param name The name
     * @return The property builder
     */
    PropertyBuilder withName(String name);

    /**
     * Sets an arbitrary property to register with the host component
     * @param name The property name
     * @param value The property value
     * @return The property builder
     */
    PropertyBuilder withProperty(String name, String value);
}
