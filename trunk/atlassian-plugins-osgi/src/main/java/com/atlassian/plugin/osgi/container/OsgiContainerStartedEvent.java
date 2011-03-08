package com.atlassian.plugin.osgi.container;

/**
 * Event fired when the OSGi container has started
 *
 * @since 2.5.0
 */
public class OsgiContainerStartedEvent
{
    private final OsgiContainerManager osgiContainerManager;

    public OsgiContainerStartedEvent(OsgiContainerManager osgiContainerManager)
    {
        this.osgiContainerManager = osgiContainerManager;
    }

    public OsgiContainerManager getOsgiContainerManager()
    {
        return osgiContainerManager;
    }
}
