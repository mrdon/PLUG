package com.atlassian.plugin.hostcontainer;

/**
 * Provides static access to a {@link HostContainer} instance.  Requires initialisation before first use.
 *
 * @since 2.2.0
 */
public abstract class HostContainerAccessor
{
    private static HostContainerAccessor hostContainerAccessor;

    /**
     * Gets the host container for instance or thread
     *
     * @return The host container instance
     * @throws IllegalStateException If it hasn't been initialised yet
     */
    public static HostContainer getHostContainer() throws IllegalStateException
    {
        if (hostContainerAccessor == null)
        {
            throw new IllegalStateException("The host container accessor has not yet been initialised");
        }
        
        return hostContainerAccessor.findHostContainer();
    }

    /**
     * Sets the implementation of the host container accessor
     *
     * @param hostContainerAccessor The implementation
     */
    public static void setHostContainerAccessor(HostContainerAccessor hostContainerAccessor)
    {
        HostContainerAccessor.hostContainerAccessor = hostContainerAccessor;
    }

    /**
     * Retrieves the host container.  Must be implemented for subclasses.
     *
     * @return The instance.  Should not be null.
     */
    protected abstract HostContainer findHostContainer();
}
