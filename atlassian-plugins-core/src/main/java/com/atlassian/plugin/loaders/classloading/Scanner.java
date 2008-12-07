package com.atlassian.plugin.loaders.classloading;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Scans the filesystem for changed or added plugin jars and stores a map of the currently known ones.
 */
public class Scanner
{
    private static Log log = LogFactory.getLog(Scanner.class);

    /**
     * File filter used to load just the jars.
     */
    private final static FileFilter fileFilter = new JarFileFilter();

    /**
     * Tracks the classloading
     */
    private File libDir;

    /**
     * A Map of {@link String} absolute file paths to {@link DeploymentUnit}s.
     */
    private Map<String,DeploymentUnit> scannedDeploymentUnits = new HashMap<String,DeploymentUnit>();


    /**
     * Constructor for scanner.
     *
     * @param libDir
     */
    public Scanner(File libDir)
    {
        this.libDir = libDir;
    }

    private DeploymentUnit createAndStoreDeploymentUnit(File file) throws MalformedURLException
    {
        if (isScanned(file))
            return null;

        DeploymentUnit unit = new DeploymentUnit(file);
        scannedDeploymentUnits.put(file.getAbsolutePath(), unit);

        return unit;
    }

    /**
     * Given a file, finds the deployment unit for it if one has already been scanned.
     * @param file a jar file.
     * @return the stored deploymentUnit matching the file or null if none exists.
     */
    public DeploymentUnit locateDeploymentUnit(File file)
    {
        return scannedDeploymentUnits.get(file.getAbsolutePath());
    }

    /**
     * Finds whether the given file has been scanned already.
     */
    private boolean isScanned(File file)
    {
        return locateDeploymentUnit(file) != null;
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
        // Checks to see if we have deleted any of the deployment units.
        List<File> removedFiles = new ArrayList<File>();
        for (DeploymentUnit unit : scannedDeploymentUnits.values())
        {
            if (!unit.getPath().exists() || !unit.getPath().canRead())
            {
                removedFiles.add(unit.getPath());
            }
        }
        clear(removedFiles);

        // Checks for new files.
        Collection<DeploymentUnit> result = new TreeSet<DeploymentUnit>(new Comparator<DeploymentUnit>()
        {
            public int compare(DeploymentUnit o1, DeploymentUnit o2)
            {
                return o1.getPath().getName().compareTo(o2.getPath().getName());
            }
        });

        File files[] = libDir.listFiles(fileFilter);
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
                        DeploymentUnit unit = createAndStoreDeploymentUnit(file);
                        if (unit != null)
                            result.add(unit);
                    } else if (!isScanned(file))
                    {
                        DeploymentUnit unit = createAndStoreDeploymentUnit(file);
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
        DeploymentUnit unit = locateDeploymentUnit(file);
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
     */
    public Collection<DeploymentUnit> getDeploymentUnits()
    {
        return Collections.unmodifiableCollection(scannedDeploymentUnits.values());
    }

    /**
     * Clears the list of scanned deployment units.
     */
    public void clearAll()
    {
        scannedDeploymentUnits.clear();
    }

    /**
     * Reinvents the wheel and lets only files ending in .jar pass through.
     */
    static class JarFileFilter implements FileFilter
    {
        public boolean accept(File file)
        {
            return file.getName().endsWith(".jar");
        }
    }
}
