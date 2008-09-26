package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.parsers.DescriptorParserFactory;
import com.atlassian.plugin.PluginParseException;

import java.io.InputStream;

import org.apache.commons.lang.Validate;

/**
 * Descriptor parser factory that creates parsers that ignore all component-related modules
 */
public class ComponentFilteringXmlDescriptorParserFactory implements DescriptorParserFactory
{
    /**
     * Gets an instance that filters the modules "component", "bean", and "spring"
     * @param source The descriptor source
     * @return The parser
     * @throws PluginParseException
     */
    public DescriptorParser getInstance(InputStream source) throws PluginParseException
    {
        Validate.notNull(source, "The descriptor source must not be null");
        return new FilteringXmlDescriptorParser(source, "component", "component-import", "bean", "spring", "module-type");
    }
}