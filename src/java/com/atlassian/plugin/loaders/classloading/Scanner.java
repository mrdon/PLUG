package com.atlassian.plugin.loaders.classloading;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Deployment Scanner
 *
 * @author <a href="vsalaman@softekpr.com">Victor Salaman</a>
 * @version 1.0
 */
public class Scanner implements FileFilter
{
    private static Log log = LogFactory.getLog(Scanner.class);

    /**
     * Tracks the classloading
     */
    File libDir;

    /**
     * Tracks classloading modifications. Keeps a value of last modified for the plugin dir.
     */
    long lastModified;

    /**
     * Holds the plugin extension.
     */
    String pluginExtension = ".jar";

    /**
     * Holds the classloaders keyed by deployment units.
     */
    Map deployedLoaders = new HashMap();

    /**
     * Constructor for scanner.
     *
     * @param libDir
     */
    public Scanner(File libDir)
    {
        this.libDir = libDir;
    }

    public boolean accept(File file)
    {
        return file.getName().endsWith(pluginExtension);
    }

    private void deploy(File file) throws MalformedURLException
    {
        if (isDeployed(file)) return;

        DeploymentUnit unit = new DeploymentUnit(file);
        JarClassLoader cl = new JarClassLoader(file, Thread.currentThread().getContextClassLoader());
        deployedLoaders.put(unit, cl);

        /** Your deploy stuff here **/
    }

    public DeploymentUnit locateDeploymentUnit(File file)
    {
        Collection dUnits = deployedLoaders.keySet();
        for (Iterator iterator = dUnits.iterator(); iterator.hasNext();)
        {
            DeploymentUnit unit = (DeploymentUnit) iterator.next();
            if (unit.path.getAbsolutePath().equals(file.getAbsolutePath()))
            {
                return unit;
            }
        }
        return null;
    }

    public boolean isDeployed(File file)
    {
        return locateDeploymentUnit(file) != null;
    }

    public void undeploy(File file)
    {
        DeploymentUnit unit = locateDeploymentUnit(file);
        if (unit != null)
        {
            JarClassLoader jcl = (JarClassLoader) deployedLoaders.remove(unit);
            jcl.close();
        }
    }

    public boolean isModified()
    {
        return libDir.canRead() && (lastModified != libDir.lastModified());
    }

    /**
     * Scans the plugin classloading and does the proper things.
     * Handles deployment and undeployment.
     */
    public void scan()
    {
        // Checks to see if we have deleted one of the deployment units.
        Collection dUnits = deployedLoaders.keySet();
        List toUndeploy = new ArrayList();
        for (Iterator iterator = dUnits.iterator(); iterator.hasNext();)
        {
            DeploymentUnit unit = (DeploymentUnit) iterator.next();
            if (!unit.path.exists() || !unit.path.canRead())
            {
                toUndeploy.add(unit.getPath());
            }
        }
        undeploy(toUndeploy);

        // Checks for new files.
        File file[] = libDir.listFiles(this);
        for (int i = 0; i < file.length; i++)
        {
            try
            {
                if (isDeployed(file[i]) && isModified(file[i]))
                {
                    undeploy(file[i]);
                    deploy(file[i]);
                }
                else if (!isDeployed(file[i]))
                {
                    deploy(file[i]);
                }
            }
            catch (MalformedURLException e)
            {
                // Change this to log somewhere
                e.printStackTrace();
            }
        }
    }

    private boolean isModified(File file)
    {
        DeploymentUnit unit = locateDeploymentUnit(file);
        return file.lastModified() > unit.lastModified();
    }

    private void undeploy(List toUndeploy)
    {
        for (Iterator iterator = toUndeploy.iterator(); iterator.hasNext();)
        {
            undeploy((File) iterator.next());
        }
    }

    public Collection getDeploymentUnits()
    {
        return deployedLoaders.keySet();
    }

    public ClassLoader getClassLoader(DeploymentUnit deploymentUnit)
    {
        return (ClassLoader) deployedLoaders.get(deploymentUnit);
    }

    public void undeployAll()
    {
        for (Iterator iterator = deployedLoaders.values().iterator(); iterator.hasNext();)
        {
            JarClassLoader jarClassLoader = (JarClassLoader) iterator.next();
            jarClassLoader.close();
            iterator.remove();
        }
    }
}
