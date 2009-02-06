package com.atlassian.plugin.parsers;

import com.atlassian.plugin.PluginParseException;

import java.io.InputStream;

/**
 * A factory for creating descriptor parsers.
 *
 * @see DescriptorParser
 * @see XmlDescriptorParserFactory
 */
public interface DescriptorParserFactory
{
    /**
     * Creates a new {@link DescriptorParser} for getting plugin descriptor information
     * from the provided source data.
     *
     * @param source the stream of data which represents the descriptor. The stream will
     * only be read once, so it need not be resettable.
     * @return an instance of the descriptor parser tied to this InputStream
     * @throws PluginParseException if there was a problem creating the descriptor parser
     * due to an invalid source stream.
     * @deprecated Since 2.2.0, use {@link #getInstance(InputStream,String)} instead
     */
    @Deprecated
    DescriptorParser getInstance(InputStream source) throws PluginParseException;

    /**
     * Creates a new {@link DescriptorParser} for getting plugin descriptor information
     * from the provided source data that knows which application it is parsing for.
     *
     * @param source the stream of data which represents the descriptor. The stream will
     * only be read once, so it need not be resettable.
     * @param applicationKey the identifier of the current application to use to match modules, if specified.  Null to
     * match everything.
     * @return an instance of the descriptor parser tied to this InputStream
     * @throws PluginParseException if there was a problem creating the descriptor parser
     * due to an invalid source stream.
     * @since 2.2.0
     */
    DescriptorParser getInstance(InputStream source, String applicationKey) throws PluginParseException;
}
