package com.atlassian.plugin.hostcontainer;

import org.apache.commons.lang.Validate;

/**
 * Singleton host container accessor that always returns the instance it was instantiated with.
 *
 * @since 2.2.0
 */
public class SingletonHostContainerAccessor extends HostContainerAccessor
{
    private final HostContainer hostContainer;

    /**
     * Constructs an accessor with a container
     *
     * @param hostContainer The host container
     * @throws IllegalArgumentException If the host container is null
     */
    public SingletonHostContainerAccessor(HostContainer hostContainer) throws IllegalArgumentException
    {
        Validate.notNull(hostContainer);
        this.hostContainer = hostContainer;
    }

    /**
     * @return the host container
     */
    protected HostContainer findHostContainer()
    {
        return hostContainer;
    }
}
