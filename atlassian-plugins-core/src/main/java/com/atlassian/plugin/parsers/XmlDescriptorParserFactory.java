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
    /**
     * @deprecated Since 2.2.0
     */
    @Deprecated
    public DescriptorParser getInstance(InputStream source) throws PluginParseException
    {
        return getInstance(source, null);
    }

    /**
     *
     * @param source the stream of data which represents the descriptor. The stream will
     * only be read once, so it need not be resettable.
     * @param applicationKey the identifier of the current application to use to match modules, if specified.  Null to
     * match everything.
     * @return
     * @throws PluginParseException
     * @since 2.2.0
     */
    public DescriptorParser getInstance(InputStream source, String applicationKey) throws PluginParseException
    {
        return new XmlDescriptorParser(source, applicationKey);
    }
}
