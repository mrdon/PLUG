package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.parsers.XmlDescriptorParser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.ModuleDescriptorFactory;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;

import org.dom4j.Element;
import org.apache.commons.lang.Validate;

/**
 * Descriptor parser that handles special tasks for osgi plugins such as ignoring certain modules and recording the
 * originating module descriptor elements.  Must only be used with {@link OsgiPlugin} instances.
 *
 * @since 2.1.2
 */
public class OsgiPluginXmlDescriptorParser extends XmlDescriptorParser
{
    private HashSet<String> modulesToIgnore;

    /**
     * @param source The XML descriptor source
     * @param modulesToIgnore A list of module types to ignore
     * 
     * @throws com.atlassian.plugin.PluginParseException
     *          if there is a problem reading the descriptor from the XML {@link java.io.InputStream}.
     */
    public OsgiPluginXmlDescriptorParser(InputStream source, String... modulesToIgnore) throws PluginParseException
    {
        super(source);
        Validate.notNull(source, "The descriptor source must not be null");
        this.modulesToIgnore = new HashSet<String>(Arrays.asList(modulesToIgnore));
    }

    /**
     * Ignores matched modules and passes module descriptor elements back to the {@link OsgiPlugin}
     * @param plugin The plugin
     * @param element The module element
     * @param moduleDescriptorFactory The module descriptor factory
     * @return The module, or null if the module cannot be found or is being ignored
     * @throws PluginParseException
     */
    @Override
    protected ModuleDescriptor createModuleDescriptor(Plugin plugin, Element element, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        ModuleDescriptor descriptor = null;
        if (!modulesToIgnore.contains(element.getName()))
        {
            descriptor = super.createModuleDescriptor(plugin, element, moduleDescriptorFactory);
            String key = (descriptor != null ? descriptor.getKey() : element.attributeValue("key"));
            ((OsgiPlugin)plugin).addModuleDescriptorElement(key, element);
        }
        return descriptor;
    }
}
