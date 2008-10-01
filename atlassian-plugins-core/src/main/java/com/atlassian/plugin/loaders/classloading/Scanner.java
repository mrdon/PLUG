package com.atlassian.plugin.loaders.classloading;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;

import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.artifact.PluginArtifactFactory;
import com.atlassian.plugin.artifact.DefaultPluginArtifactFactory;

/**
 * Scans the filesystem for changed or added plugin files and stores a map of the currently known ones.
 */
public class Scanner
{
    private static Log log = LogFactory.getLog(Scanner.class);

    /**
     * Tracks the classloading
     */
    private final File libDir;

    /**
     * A Map of {@link String} absolute file paths to {@link PluginArtifact}s.
     */
    private final Map<String,PluginArtifact> scannedDeploymentUnits = new HashMap<String, PluginArtifact>();
    private final PluginArtifactFactory pluginArtifactFactory;


    /**
     * @deprecated Since 2.1.0
     */
    public Scanner(File libDir)
    {
        this(libDir, new DefaultPluginArtifactFactory());
    }

    /**
     *
     * @param libDir
     * @param pluginArtifactFactory
     * @since 2.1.0
     */
    public Scanner(File libDir, PluginArtifactFactory pluginArtifactFactory)
    {
        this.pluginArtifactFactory = pluginArtifactFactory;
        this.libDir = libDir;
    }

    private PluginArtifact createAndStoreDeploymentUnit(File file) throws MalformedURLException
    {
        if (isScanned(file))
            return null;

        PluginArtifact unit;
        try
        {
            unit = pluginArtifactFactory.create(file);
        }
        catch (IllegalArgumentException ex)
        {
            log.warn(ex);
            return null;
        }
        scannedDeploymentUnits.put(file.getAbsolutePath(), unit);

        return unit;
    }

    /**
     * @deprecated Since 2.1.0, use {@link #locatePluginArtifact(File)} instead
     */
    public DeploymentUnit locateDeploymentUnit(File file)
    {
        return new DeploymentUnit(locatePluginArtifact(file));
    }

    /**
     * Given a file, finds the deployment unit for it if one has already been scanned.
     * @param file a jar file.
     * @return the stored deploymentUnit matching the file or null if none exists.
     * @since 2.1.0
     */
    public PluginArtifact locatePluginArtifact(File file)
    {
        return scannedDeploymentUnits.get(file.getAbsolutePath());
    }

    /**
     * Finds whether the given file has been scanned already.
     */
    private boolean isScanned(File file)
    {
        return locatePluginArtifact(file) != null;
    }

    /**
     * Tells the Scanner to forget about a file it has loaded so that it will reload it
     * next time it scans.
     *
     * @param file a file that may have already been scanned.
     */
    public void clear(File file)
    {
        scannedDeploymentUnits.remove(file.getAbsolutePath());
    }

    /**
     * Scans for jars that have been added or modified since the last call to scan.
     *
     * @return Collection of {@link DeploymentUnit}s that describe newly added Jars.
     */
    public Collection<DeploymentUnit> scan()
    {
        Collection<PluginArtifact> artifacts = scanForArtifacts();
        return convertArtifactsToDeploymentUnits(artifacts);
    }

    private Collection<DeploymentUnit> convertArtifactsToDeploymentUnits(Collection<PluginArtifact> artifacts)
    {
        Collection<DeploymentUnit> units = new ArrayList<DeploymentUnit>();
        for (PluginArtifact artifact : artifacts)
        {
            units.add(new DeploymentUnit(artifact));
        }
        return units;
    }

    /**
     * Scans for jars that have been added or modified since the last call to scan.
     *
     * @return Collection of {@link DeploymentUnit}s that describe newly added Jars.
     */
    public Collection<PluginArtifact> scanForArtifacts()
    {
        // Checks to see if we have deleted any of the deployment units.
        List<File> removedFiles = new ArrayList<File>();
        for (PluginArtifact unit : scannedDeploymentUnits.values())
        {
            if (!unit.getFile().exists() || !unit.getFile().canRead())
            {
                removedFiles.add(unit.getFile());
            }
        }
        clear(removedFiles);

        // Checks for new files.
        Collection<PluginArtifact> result = new ArrayList<PluginArtifact>();
        File files[] = libDir.listFiles();
        if (files == null)
        {
            log.error("listFiles returned null for directory " + libDir.getAbsolutePath());
        }
        else
        {
            for (File file : files)
            {
                try
                {
                    if (isScanned(file) && isModified(file))
                    {
                        clear(file);
                        PluginArtifact unit = createAndStoreDeploymentUnit(file);
                        if (unit != null)
                            result.add(unit);
                    } else if (!isScanned(file))
                    {
                        PluginArtifact unit = createAndStoreDeploymentUnit(file);
                        if (unit != null)
                            result.add(unit);
                    }
                }
                catch (MalformedURLException e)
                {
                    log.error("Error deploying plugin " + file.getAbsolutePath(), e);
                }
            }
        }
        return result;
    }

    private boolean isModified(File file)
    {
        PluginArtifact unit = locatePluginArtifact(file);
        return file.lastModified() > unit.lastModified();
    }

    private void clear(List<File> toUndeploy)
    {
        for (File aToUndeploy : toUndeploy)
        {
            clear( aToUndeploy);
        }
    }

    /**
     * Retrieve all the {@link DeploymentUnit}s currently stored.
     *
     * @return the complete unmodifiable list of scanned {@link DeploymentUnit}s.
     * @since 2.1.0
     */
    public Collection<PluginArtifact> getPluginArtifacts()
    {
        return Collections.unmodifiableCollection(scannedDeploymentUnits.values());
    }

    /**
     * @deprecated Since 2.1.0, use {@link #getPluginArtifacts()} instead
     */
    public Collection<DeploymentUnit> getDeploymentUnits()
    {
        return convertArtifactsToDeploymentUnits(getPluginArtifacts());
    }

    /**
     * Clears the list of scanned deployment units.
     */
    public void clearAll()
    {
        scannedDeploymentUnits.clear();
    }
}
