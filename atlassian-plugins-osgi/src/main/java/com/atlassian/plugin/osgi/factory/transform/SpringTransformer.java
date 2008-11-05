package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.PluginParseException;
import org.dom4j.Document;

import java.util.List;
import java.util.jar.Manifest;
import java.io.File;

/**
 * Converts a piece of the plugin descriptor into Spring configuration
 * @since 2.1
 */
public interface SpringTransformer
{
    /**
     * Transforms data into Spring configuration
     *
     * @param regs The list of host components
     * @param pluginDoc The plugin document
     * @param springDoc The spring document to write the configuration into
     */
    void transform(File jar, Manifest manifest, List<HostComponentRegistration> regs, Document pluginDoc, Document springDoc)
        throws PluginParseException;

}