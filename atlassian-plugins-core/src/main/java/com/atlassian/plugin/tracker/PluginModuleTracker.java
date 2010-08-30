package com.atlassian.plugin.tracker;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * Tracks enabled plugin module descriptors, focusing on fast reads.  Patterned off the
 * {@link org.osgi.util.tracker.ServiceTracker}.
 *
 * @since 2.6.0
 */
public interface PluginModuleTracker<T extends ModuleDescriptor>
{
    /**
     * Implement this to customize how and which descriptors are stored
     * @param <T> The descriptor type
     */
    interface Customizer<T extends ModuleDescriptor>
    {
        /**
         * Called before adding the descriptor to the internal tracker
         * @param descriptor The new descriptor
         * @return The descriptor to track
         */
        T adding(T descriptor);

        /**
         * Called after the descriptor has been removed from the internal tracker
         * @param descriptor The descriptor that was removed
         */
        void removed(T descriptor);

    }

    /**
     * @return a live view of enabled module descriptors
     */
    Iterable<T> getModuleDescriptors();

    /**
     * Gets a live view of enabled module instances
     * @param moduleClass The module class, for type safety
     * @param <MT> The type of module class
     * @return The module instances
     */
    <MT> Iterable<MT> getModules(Class<MT> moduleClass);

    /**
     * @return The number of module descriptors currently tracked.
     */
    int size();

    /**
     * Closes the tracker.  Ensure you call this, or you may cause a memory leak.
     */
    void close();
}
