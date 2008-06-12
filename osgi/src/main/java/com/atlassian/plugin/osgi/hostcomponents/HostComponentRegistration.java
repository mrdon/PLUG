package com.atlassian.plugin.osgi.hostcomponents;

import java.util.Dictionary;

public interface HostComponentRegistration
{
    Dictionary<String, String> getProperties();

    String[] getMainInterfaces();

    Object getInstance();
}
