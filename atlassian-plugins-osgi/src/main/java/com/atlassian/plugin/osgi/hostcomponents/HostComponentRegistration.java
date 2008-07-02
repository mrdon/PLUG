package com.atlassian.plugin.osgi.hostcomponents;

import java.util.Dictionary;

/**
 * Represents a registration of a host component
 */
public interface HostComponentRegistration
{
    /**
     * @return The metadata properties for the component
     */
    Dictionary<String, String> getProperties();

    /**
     * @return A list of interface names
     */
    String[] getMainInterfaces();

    /**
     * @return The component instance
     */
    Object getInstance();
}
