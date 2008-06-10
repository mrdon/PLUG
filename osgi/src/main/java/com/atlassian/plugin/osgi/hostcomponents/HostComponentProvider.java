package com.atlassian.plugin.osgi.hostcomponents;

public interface HostComponentProvider
{

    void provide(ComponentRegistrar registrar);
}
