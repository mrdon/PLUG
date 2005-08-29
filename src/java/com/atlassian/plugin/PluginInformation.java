/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 31, 2004
 * Time: 12:58:29 PM
 */
package com.atlassian.plugin;

import java.util.Map;
import java.util.HashMap;

public class PluginInformation
{
    private String description;
    private String version;
    private String vendorName;
    private String vendorUrl;
    private float maxVersion;
    private float minVersion;
    private Map parameters = new HashMap();

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }

    public void setVendorUrl(String vendorUrl)
    {
        this.vendorUrl = vendorUrl;
    }

    public String getVendorName()
    {
        return vendorName;
    }

    public String getVendorUrl()
    {
        return vendorUrl;
    }


    public void setMaxVersion(float maxVersion)
    {
        this.maxVersion = maxVersion;
    }

    public void setMinVersion(float minVersion)
    {
        this.minVersion = minVersion;
    }

    public float getMaxVersion()
    {
        return maxVersion;
    }

    public float getMinVersion()
    {
        return minVersion;
    }

    public Map getParameters()
    {
        return parameters;
    }

    public void addParameter(Object key, Object value)
    {
        this.parameters.put(key, value);
    }
}