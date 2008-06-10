package com.atlassian.plugin.osgi.hostcomponents;

public interface ComponentRegistrar
{
    static final String HOST_COMPONENT_FLAG = "plugins.host";
    
    <T> InstanceBuilder<T> register(Class<T>... mainInterface);
}
