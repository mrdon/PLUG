package com.atlassian.plugin.loaders.classloading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private final File libDir;

    /**
     * A Map of {@link String} absolute file paths to {@link DeploymentUnit}s.
     */
    private final Map<String,DeploymentUnit> scannedDeploymentUnits = new HashMap<String,DeploymentUnit>();


    /**
     * Constructor for scanner.
     *
     * @param libDir
     */
    public Scanner(final File libDir)
    {
        this.libDir = libDir;
    }

    private DeploymentUnit createAndStoreDeploymentUnit(final File file)
    {
        if (isScanned(file))
            return null;

        final DeploymentUnit unit = new DeploymentUnit(file);
        scannedDeploymentUnits.put(file.getAbsolutePath(), unit);

        return unit;
    }

    /**
     * Given a file, finds the deployment unit for it if one has already been scanned.
     * @param file a jar file.
     * @return the stored deploymentUnit matching the file or null if none exists.
     */
    public DeploymentUnit locateDeploymentUnit(final File file)
    {
        return scannedDeploymentUnits.get(file.getAbsolutePath());
    }

    /**
     * Finds whether the given file has been scanned already.
     */
    private boolean isScanned(final File file)
    {
        return locateDeploymentUnit(file) != null;
    }

    /**
     * Tells the Scanner to forget about a file it has loaded so that it will reload it
     * next time it scans.
     *
     * @param file a file that may have already been scanned.
     */
    public void clear(final File file)
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
        final List<File> removedFiles = new ArrayList<File>();
        for (final DeploymentUnit unit : scannedDeploymentUnits.values())
        {
            if (!unit.getPath().exists() || !unit.getPath().canRead())
            {
                removedFiles.add(unit.getPath());
            }
        }
        clear(removedFiles);

        // Checks for new files.
        final Collection<DeploymentUnit> result = new TreeSet<DeploymentUnit>(new Comparator<DeploymentUnit>()
        {
            public int compare(final DeploymentUnit o1, final DeploymentUnit o2)
            {
                return o1.getPath().getName().compareTo(o2.getPath().getName());
            }
        });

        final File files[] = libDir.listFiles(fileFilter);
        if (files == null)
        {
            log.error("listFiles returned null for directory " + libDir.getAbsolutePath());
        }
        else
        {
            for (final File file : files)
            {
                if (isScanned(file) && isModified(file))
                {
                    clear(file);
                    final DeploymentUnit unit = createAndStoreDeploymentUnit(file);
                    if (unit != null)
                        result.add(unit);
                } else if (!isScanned(file))
                {
                    final DeploymentUnit unit = createAndStoreDeploymentUnit(file);
                    if (unit != null)
                        result.add(unit);
                }
            }
        }
        return result;
    }

    private boolean isModified(final File file)
    {
        final DeploymentUnit unit = locateDeploymentUnit(file);
        return file.lastModified() > unit.lastModified();
    }

    private void clear(final List<File> toUndeploy)
    {
        for (final File aToUndeploy : toUndeploy)
        {
            clear( aToUndeploy);
        }
    }

    LoadingOrderComparator loadingOrderComparator;

    LoadingOrderComparator getLoadingOrderComparator()
    {
        if (loadingOrderComparator == null)
        {
            loadingOrderComparator = new LoadingOrderComparator(new File(libDir, "loadingorder.txt"));
        }
        return loadingOrderComparator;
    }

    private class LoadingOrderComparator implements Comparator<DeploymentUnit>
    {
        private final List<String> pluginOrder = new ArrayList<String>();

        public LoadingOrderComparator(final File file)
        {
            if (file.exists() && file.canRead())
            {
                BufferedReader in = null;
                try
                {
                    in = new BufferedReader(new FileReader(file));
                    String str;
                    while ((str = in.readLine()) != null)
                    {
                        pluginOrder.add(str);
                    }
                } catch (final IOException e)
                {
                    log.debug("Error reading loadingorder.txt file", e);
                } finally
                {
                    IOUtils.closeQuietly(in);
                }
            }
        }

        public int compare(final DeploymentUnit o1, final DeploymentUnit o2)
        {
            final String name1 = o1.getPath().getName();
            final String name2 = o2.getPath().getName();

            final int index1 = pluginOrder.indexOf(name1);
            final int index2 = pluginOrder.indexOf(name2);
            if (index1 == -1)
                return 1;
            if (index2 == -1)
                return -1;

            return index1 < index2 ? -1 : index1 > index2 ? 1 : 0;
        }
    }

    /**
     * Retrieve all the {@link DeploymentUnit}s currently stored.
     *
     * @return the complete unmodifiable list of scanned {@link DeploymentUnit}s.
     */
    public Collection<DeploymentUnit> getDeploymentUnits()
    {
        final List<DeploymentUnit> list = new ArrayList<DeploymentUnit>(scannedDeploymentUnits.values());
        Collections.sort(list, getLoadingOrderComparator());
        log.debug("Plugin loading order: " + list);
        return Collections.unmodifiableCollection(list);
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
        public boolean accept(final File file)
        {
            return file.getName().endsWith(".jar");
        }
    }
}
