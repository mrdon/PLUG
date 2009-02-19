package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.PluginArtifact;

import java.io.File;
import java.util.List;

/**
 * Transforms a plugin jar into a proper OSGi bundle
 */
public interface PluginTransformer
{
    /**
     * Transforms a plugin artifact into a proper OSGi bundle
     *
     * @param pluginArtifact The plugin artifact
     * @param regs The list of registered host components
     * @return The transformed OSGi bundle
     * @throws PluginTransformationException If anything goes wrong
     * @since 2.2.0
     */
    File transform(PluginArtifact pluginArtifact, List<HostComponentRegistration> regs) throws PluginTransformationException;
}
