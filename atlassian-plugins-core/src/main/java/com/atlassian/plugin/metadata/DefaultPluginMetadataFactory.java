package com.atlassian.plugin.metadata;

import net.jcip.annotations.ThreadSafe;

import com.atlassian.plugin.metadata.PluginMetadata.Factory;
import com.atlassian.plugin.metadata.PluginMetadata.Type;
import com.google.common.base.Preconditions;

/**
 * Default implementation of the {@link PluginMetadata.Factory} that uses a
 * {@link MetadataLoader loader} to get the names for the specific
 * {@link Type types}.
 * 
 * @since 2.6
 */
@ThreadSafe
final class DefaultPluginMetadataFactory implements PluginMetadata.Factory
{
    /**
     * Creates a {@link PluginMetadata.Factory} that access the class-path to
     * find files
     * 
     * @return a {@link Factory} that reads from the class path.
     */
    static PluginMetadata.Factory fromClasspath()
    {
        return new DefaultPluginMetadataFactory(DefaultMetadataLoader.loadFromClasspath());
    }

    private final MetadataLoader loader;

    DefaultPluginMetadataFactory(final MetadataLoader loader)
    {
        this.loader = Preconditions.checkNotNull(loader, "loader");
    }

    public PluginMetadata get()
    {
        final Iterable<String> applicationPlugins = loader.load(Type.ApplicationProvided);
        final Iterable<String> requiredPlugins = loader.load(Type.RequiredPlugins);
        final Iterable<String> requiredModules = loader.load(Type.RequiredModuleDescriptors);

        return new DefaultPluginMetadata(applicationPlugins, requiredPlugins, requiredModules);
    }
}
