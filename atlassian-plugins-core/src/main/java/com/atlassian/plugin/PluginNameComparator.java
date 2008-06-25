package com.atlassian.plugin;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares two plugins by their names
 */
class PluginNameComparator implements Comparator, Serializable
{
    static final long serialVersionUID = -2595168544386708474L;

    /**
     * Gets names of the two given plugins and returns the result of their comparison
     *
     * @param o1 plugin to compare
     * @param o2 plugin to compare
     * @return result of plugin name comparison
     */
    public int compare(Object o1, Object o2)
    {
        Plugin p1 = (Plugin) o1;
        Plugin p2 = (Plugin) o2;
        return p1.getName().compareTo(p2.getName());
    }
}
