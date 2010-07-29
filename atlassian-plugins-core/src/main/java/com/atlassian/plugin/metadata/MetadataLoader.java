package com.atlassian.plugin.metadata;

import com.atlassian.plugin.metadata.PluginMetadata.Type;
import com.atlassian.util.concurrent.NotNull;

/**
 * Loads the names of described plugins and module descriptors.
 * 
 * @since 2.6
 */
interface MetadataLoader
{
    /**
     * Load the set of names for a particular type of metadata.
     * 
     * @param the type of metadata to load
     * @return a non-null iterable of non-blank or null names
     */
    @NotNull
    Iterable<String> load(Type type);
}
