package com.atlassian.plugin.osgi.loader;

import com.atlassian.plugin.parsers.XmlDescriptorParser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.ModuleDescriptorFactory;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Arrays;

import org.dom4j.Element;

/**
 * Descriptor parser that ignores certain modules
 */
public class FilteringXmlDescriptorParser extends XmlDescriptorParser
{
    private HashSet<String> modulesToIgnore;

    /**
     * @throws com.atlassian.plugin.PluginParseException
     *          if there is a problem reading the descriptor from the XML {@link java.io.InputStream}.
     */
    public FilteringXmlDescriptorParser(InputStream source, String... modulesToIgnore) throws PluginParseException
    {
        super(source);
        this.modulesToIgnore = new HashSet<String>(Arrays.asList(modulesToIgnore));
    }

    /**
     * Ignores matched modules, otherwise, the normal behavior occurs
     * @param plugin The plugin
     * @param element The module element
     * @param moduleDescriptorFactory The module descriptor factory
     * @return The module, or null if the module cannot be found or is being ignored
     * @throws PluginParseException
     */
    @Override
    protected ModuleDescriptor createModuleDescriptor(Plugin plugin, Element element, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        if (!modulesToIgnore.contains(element.getName()))
        {
            return super.createModuleDescriptor(plugin, element, moduleDescriptorFactory);
        } else
        {
            return null;
        }
    }
}
