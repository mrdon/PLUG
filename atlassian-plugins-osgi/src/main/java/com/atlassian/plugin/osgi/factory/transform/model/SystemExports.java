package com.atlassian.plugin.osgi.factory.transform.model;

import aQute.lib.header.OSGiHeader;

import java.util.Map;

public class SystemExports
{
    private final Map<String, Map<String,String>> exports;

    public SystemExports(String exportsLine)
    {
        this.exports = OSGiHeader.parseHeader(exportsLine);
    }

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
