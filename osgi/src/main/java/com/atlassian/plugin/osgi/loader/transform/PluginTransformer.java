package com.atlassian.plugin.osgi.loader.transform;

import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

import java.io.File;
import java.util.List;

public interface PluginTransformer
{
    File transform(File pluginJar, List<HostComponentRegistration> regs) throws PluginTransformationException;
}
