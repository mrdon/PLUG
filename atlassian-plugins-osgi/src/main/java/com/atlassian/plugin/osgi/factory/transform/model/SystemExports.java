package com.atlassian.plugin.osgi.factory.transform.model;

import aQute.lib.header.OSGiHeader;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.lang.Validate;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;

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
        this.exports = Collections.unmodifiableMap(OsgiHeaderUtil.parseHeader(exportsLine));
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
        if (exports.containsKey(pkg))
        {
            Map<String,String> attrs = new HashMap<String,String>(exports.get(pkg));
            if (attrs.containsKey("version"))
            {
                final String version = attrs.get("version");
                attrs.put("version", "[" + version + "," + version + "]");
            }
            return OsgiHeaderUtil.buildHeader(pkg, attrs);
        }
        return pkg;
    }
}
