package com.atlassian.plugin.osgi.container;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.twdata.pkgscanner.ExportPackage;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

/**
 * Manages the OSGi container and handles any interactions with it
 */
public interface OsgiContainerManager
{
    /**
     * Starts the OSGi container
     *
     * @param provider The host component provider to use when registering host services
     * @throws OsgiContainerException If the container cannot be started
     */
    void start(HostComponentProvider provider) throws OsgiContainerException;

    /**
     * Stops the OSGi container
     *
     * @throws OsgiContainerException If the container cannot be stopped
     */
    void stop() throws OsgiContainerException;

    /**
     * Installs a bundle into a running OSGI container
     * @param file The bundle file to install
     * @return The created bundle
     * @throws OsgiContainerException If the bundle cannot be loaded
     */
    Bundle installBundle(File file) throws OsgiContainerException;

    /**
     * Reloads all host components used
     * @param provider The host component provider to use when registering host services
     */
    void reloadHostComponents(HostComponentProvider provider);

    /**
     * @return If the container is running or not
     */
    boolean isRunning();

    /**
     * Gets a list of installed bundles
     *
     * @return An array of bundles
     */
    Bundle[] getBundles();

    /**
     * Gets a list of service references
     * @return An array of service references
     */
    ServiceReference[] getRegisteredServices();

    /**
     * Gets a list of host component registrations
     *
     * @return A list of host component registrations
     */
    List<HostComponentRegistration> getHostComponentRegistrations();
}
