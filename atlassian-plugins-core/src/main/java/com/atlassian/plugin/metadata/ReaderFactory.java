package com.atlassian.plugin.metadata;

import java.io.Reader;

import com.atlassian.plugin.metadata.PluginMetadata.Type;

/**
 * Get {@link Reader readers} for a specific metadata type.
 * 
 * @since 2.6
 */
interface ReaderFactory
{
    Iterable<Reader> getReaders(Type type);
}