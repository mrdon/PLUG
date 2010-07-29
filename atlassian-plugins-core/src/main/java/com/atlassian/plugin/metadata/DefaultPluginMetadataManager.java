package com.atlassian.plugin.metadata;

import static com.google.common.base.Preconditions.checkNotNull;
import net.jcip.annotations.Immutable;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.descriptors.CannotDisable;

/**
 * A default implementation that looks at the com.atlassian.plugin.metadata
 * package of the classpath for files named:
 * <ul>
 * <li>application-provided-plugins.txt - used to list the plugin keys of all
 * plugins that are provided by the host application</li>
 * <li>application-required-plugins.txt - used to list the plugin keys that are
 * considered required for the application to function correctly</li>
 * <li>application-required-modules.txt - used to list the module keys that are
 * considered required for the application to function correctly</li>
 * </ul>
 * Note that all files in that package space with those names will be included.
 * All files contents will be used to inform this implementation of plugin keys.
 * <p>
 * Note: the actual file names are in the {@link PluginMetadata.Type} class.
 * 
 * @since 2.6
 */
@Immutable
public final class DefaultPluginMetadataManager implements PluginMetadataManager
{
    private final PluginMetadata metadata;

    /**
     * Production ctor. Loads from the class path.
     */
    public DefaultPluginMetadataManager()
    {
        this(DefaultPluginMetadataFactory.fromClasspath());
    }

    /**
     * Test ctor.
     */
    DefaultPluginMetadataManager(final PluginMetadata.Factory factory)
    {
        this.metadata = checkNotNull(checkNotNull(factory, "factory").get(), "metadata");
    }

    /**
     * A plugin is determined to be non-user if
     * {@link com.atlassian.plugin.Plugin#isBundledPlugin()} is true or if the
     * host application has indicated to the plugins system that a plugin was
     * provided by it. NOTE: If a user has upgraded a bundled plugin then the
     * decision of whether it is user installed plugin is determined by if the
     * application has indicated to the plugins system that a plugin was
     * provided or not.
     */
    public boolean isUserInstalled(final Plugin plugin)
    {
        checkNotNull(plugin, "plugin");
        // It is user installed if it has not been marked as provided by the
        // application and it was not bundled.
        return !plugin.isBundledPlugin() && !metadata.applicationProvided(plugin);
    }

    /**
     * A plugin is determined to be optional if the host application has not
     * indicated to the plugins system that it is required or if any of its
     * modules have been flagged as not optional.
     */
    public boolean isOptional(final Plugin plugin)
    {
        checkNotNull(plugin, "plugin");
        // If the application has marked the plugin as required then we know we
        // are required
        if (!optionalAccordingToHostApplication(plugin))
        {
            return false;
        }

        // We need to check if any of the plugins modules are not optional
        for (final ModuleDescriptor<?> moduleDescriptor : plugin.getModuleDescriptors())
        {
            if (!optionalAccordingToHostApplication(moduleDescriptor))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * A module is determined to be optional if the host application has not
     * indicated to the plugins system that it is required. If the call to
     * {@code isOptional} with the module descriptor's plugin is {@code false},
     * then this method will also return {@code false}. Also if the module
     * descriptor is annotated with
     * {@link com.atlassian.plugin.descriptors.CannotDisable} then it can not be
     * optional.
     */
    public boolean isOptional(final ModuleDescriptor<?> moduleDescriptor)
    {
        checkNotNull(moduleDescriptor, "moduleDescriptor");
        // It is not optional if the host application has marked it as required
        if (!optionalAccordingToHostApplication(moduleDescriptor))
        {
            return false;
        }

        // A module can not be optional if it is marked 
        // by the CannotDisable annotation
        if (!optionalAccordingToModuleDescriptorType(moduleDescriptor))
        {
            return false;
        }

        // A module can only be optional if its parent plugin is not declared by
        // the host application as required
        return optionalAccordingToHostApplication(moduleDescriptor.getPlugin());
    }

    private boolean optionalAccordingToHostApplication(final Plugin plugin)
    {
        return !metadata.required(plugin);
    }

    private boolean optionalAccordingToHostApplication(final ModuleDescriptor<?> moduleDescriptor)
    {
        return !metadata.required(moduleDescriptor);
    }

    private boolean optionalAccordingToModuleDescriptorType(final ModuleDescriptor<?> moduleDescriptor)
    {
        return !moduleDescriptor.getClass().isAnnotationPresent(CannotDisable.class);
    }
}
