package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

import java.io.File;
import java.util.List;

/**
 * Transforms a plugin jar into a proper OSGi bundle
 */
public interface PluginTransformer
{
    /**
     * Transforms a plugin jar into a proper OSGi bundle
     *
     * @param pluginJar The plugin file
     * @param regs The list of registered host components
     * @return The transformed OSGi bundle
     * @throws PluginTransformationException If anything goes wrong
     */
    File transform(File pluginJar, List<HostComponentRegistration> regs) throws PluginTransformationException;
}
