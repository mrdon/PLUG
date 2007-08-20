package com.atlassian.plugin.parsers;

import com.atlassian.plugin.PluginParseException;

import java.io.InputStream;

/**
 * Creates XML descriptor parser instances.
 *
 * @see XmlDescriptorParser
 * @see DescriptorParserFactory
 */
public class XmlDescriptorParserFactory implements DescriptorParserFactory
{
    public DescriptorParser getInstance(InputStream source) throws PluginParseException
    {
        return new XmlDescriptorParser(source);
    }
}
