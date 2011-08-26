package com.atlassian.plugin.metadata;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.descriptors.CannotDisable;
import net.jcip.annotations.Immutable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A default implementation that uses the {@link com.atlassian.plugin.metadata.ClasspathFilePluginMetadata}
 * plugin metadata implementation to resolve the application provided plugin metadata.
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
        this(new ClasspathFilePluginMetadata());
    }

    /**
     * Test ctor.
     */
    DefaultPluginMetadataManager(final PluginMetadata metadata)
    {
        this.metadata = checkNotNull(metadata, "metadata");
    }

    /**
     * <p>A plugin is determined to be non-user if
     * {@link com.atlassian.plugin.Plugin#isBundledPlugin()} is true or if the
     * host application has indicated to the plugins system that a plugin was
     * provided by it.</p>
     *
     * <p><strong>NOTE:</strong> If a user has upgraded a bundled plugin then the
     * decision of whether it is user installed plugin is determined by if the
     * application has indicated to the plugins system that a plugin was
     * provided or not.</p>
     */
    public boolean isUserInstalled(final Plugin plugin)
    {
        checkNotNull(plugin, "plugin");
        // It is user installed if it has not been marked as provided by the
        // application and it was not bundled.
        return !plugin.isBundledPlugin() && !metadata.applicationProvided(plugin);
    }

    /**
     * A plugin is determined to be &quot;system&quot; if
     * {@link #isUserInstalled(com.atlassian.plugin.Plugin)} is false.
     */
    public boolean isSystemProvided(Plugin plugin)
    {
        return !isUserInstalled(plugin);
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
