/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 31, 2004
 * Time: 12:58:29 PM
 */
package com.atlassian.plugin;

import com.atlassian.plugin.util.JavaVersionUtils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class PluginInformation
{
    private String description;
    private String descriptionKey;
    private String version;
    private String vendorName;
    private String vendorUrl;
    private float maxVersion;
    private float minVersion;
    private Float minJavaVersion;
    private Map<Object,Object> parameters = new HashMap<Object,Object>();

    public static final String NOT_SPECIFIED = "Not provided";
    public static final String NO_VERSION_SPECIFIED = "0.0";
    public static final Float NO_MIN_JAVA_VERSION = new Float(0.0);

    public static PluginInformation getInstanceWithDefaults()
    {
        PluginInformation newInfo = new PluginInformation();
        validateRequiredFields(newInfo);
        
        return newInfo;
    }
    
    public static void validateRequiredFields(PluginInformation pluginInformation)
    {
        if( StringUtils.isBlank(pluginInformation.getVendorName()))
        {
            pluginInformation.setVendorName(NOT_SPECIFIED);
        }
        
        if( StringUtils.isBlank(pluginInformation.getVersion()))
        {
            pluginInformation.setVersion(NO_VERSION_SPECIFIED);
        }
        
        if( pluginInformation.getMinJavaVersion() == null) 
        {
            pluginInformation.setMinJavaVersion(NO_MIN_JAVA_VERSION);
        }
    }
    
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

    public Float getMinJavaVersion()
    {
        return minJavaVersion;
    }

    public void setMinJavaVersion(Float minJavaVersion)
    {
        this.minJavaVersion = minJavaVersion;
    }

    public Map getParameters()
    {
        return parameters;
    }

    public void addParameter(Object key, Object value)
    {
        this.parameters.put(key, value);
    }

    public boolean satisfiesMinJavaVersion()
    {
        return minJavaVersion == null || JavaVersionUtils.satisfiesMinVersion(minJavaVersion);
    }

    public void setDescriptionKey(String descriptionKey)
    {
        this.descriptionKey = descriptionKey;
    }

    public String getDescriptionKey()
    {
        return descriptionKey;
    }

}