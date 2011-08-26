package com.atlassian.plugin.loaders;

import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * A scanner that simply scans a given set of input files.
 * This scanner will always return the units in the order supplied in the constructor.
 */
public class FileListScanner implements com.atlassian.plugin.loaders.classloading.Scanner
{
    private final Collection<File> files;
    private transient Collection<DeploymentUnit> units;

    public FileListScanner(final Collection<File> files)
    {
        this.files = new ArrayList<File>(files);
    }

    public Collection<DeploymentUnit> scan()
    {
        if (units != null) {
            return Collections.emptyList();
        }

        units = new ArrayList<DeploymentUnit>();
        for (File file : files)
        {
            units.add(new DeploymentUnit(file));
        }

        return units;
    }

    public Collection<DeploymentUnit> getDeploymentUnits()
    {
        return Collections.unmodifiableCollection(units);
    }

    public void reset()
    {
        units = null;
    }

    public void remove(final DeploymentUnit unit) throws PluginException
    {
        throw new PluginException("Cannot remove files in a file-list scanner: " + unit.getPath());
    }
}
