package com.atlassian.plugin;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares two plugins by their names
 */
class PluginNameComparator implements Comparator<Plugin>, Serializable
{
    static final long serialVersionUID = -2595168544386708474L;

    /**
     * Gets names of the two given plugins and returns the result of their comparison
     *
     * @param p1 plugin to compare
     * @param p2 plugin to compare
     * @return result of plugin name comparison
     */
    public int compare(Plugin p1, Plugin p2)
    {
        return p1.getName().compareTo(p2.getName());
    }
}
