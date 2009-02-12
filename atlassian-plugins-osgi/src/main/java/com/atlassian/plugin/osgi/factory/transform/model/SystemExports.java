package com.atlassian.plugin.osgi.factory.transform.model;

import aQute.lib.header.OSGiHeader;

import java.util.Map;

/**
 * Encapsulates the package exports from the system bundle
 *
 * @since 2.2.0
 */
public class SystemExports
{
    private final Map<String, Map<String,String>> exports;

    /**
     * Constructs an instance by parsing the exports line from the manifest
     *
     * @param exportsLine The Export-Package header value
     */
    public SystemExports(String exportsLine)
    {
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
        String fullPkg = pkg;
        if (exports.containsKey(pkg))
        {
            StringBuilder sb = new StringBuilder();
            sb.append(pkg);
            Map<String,String> attrs = exports.get(pkg);
            if (attrs != null && !attrs.isEmpty())
            {
                for (Map.Entry<String,String> entry : attrs.entrySet())
                {
                    sb.append(";");
                    sb.append(entry.getKey());
                    sb.append("=\"");

                    if ("version".equals(entry.getKey()))
                    {
                        sb.append("[").append(entry.getValue()).append(",").append(entry.getValue()).append("]");
                    }
                    else
                    {
                        sb.append(entry.getValue());
                    }
                    sb.append("\"");
                }
            }
            fullPkg = sb.toString();
        }
        return fullPkg;
    }
}
