package com.atlassian.plugin.osgi.factory.transform.model;

import aQute.lib.header.OSGiHeader;

import java.util.Map;

import org.apache.commons.lang.Validate;

/**
 * Encapsulates the package exports from the system bundle
 *
 * @since 2.2.0
 */
public class SystemExports
{
    private final Map<String, Map<String,String>> exports;

    public static final SystemExports NONE = new SystemExports("");

    /**
     * Constructs an instance by parsing the exports line from the manifest
     *
     * @param exportsLine The Export-Package header value
     */
    public SystemExports(String exportsLine)
    {
        if (exportsLine == null)
        {
            exportsLine = "";
        }
        this.exports = OSGiHeader.parseHeader(exportsLine);
    }

    /**
     * Constructs a package export, taking into account any attributes on the system export, including the version.
     * The version is handled special, in that is added as an exact match, i.e. [1.0,1.0].
     *
     * @param pkg The java package
     * @return The full export line to use for a host component import
     */
    public String getFullExport(String pkg)
    {
        StringBuilder fullPkg = new StringBuilder(pkg);
        if (exports.containsKey(pkg))
        {
            Map<String,String> attrs = exports.get(pkg);
            if (attrs != null && !attrs.isEmpty())
            {
                for (Map.Entry<String,String> entry : attrs.entrySet())
                {
                    fullPkg.append(";");
                    fullPkg.append(entry.getKey());
                    fullPkg.append("=\"");

                    if ("version".equals(entry.getKey()))
                    {
                        fullPkg.append("[").append(entry.getValue()).append(",").append(entry.getValue()).append("]");
                    }
                    else
                    {
                        fullPkg.append(entry.getValue());
                    }
                    fullPkg.append("\"");
                }
            }
        }
        return fullPkg.toString();
    }
}
