package com.atlassian.plugin.util;

/**
 * A simple utility for comparing a java version number to the running version.
 */
public class JavaVersionUtils
{
    public static boolean satisfiesMinVersion(float versionNumber)
    {
        float specVersion = Float.valueOf(System.getProperty("java.specification.version")).floatValue();
        return specVersion >= versionNumber;
    }

    public static Float resolveVersionFromString(String versionStr)
    {
        try
        {
            return Float.valueOf(versionStr);
        }
        catch(Exception e)
        {
            return null;
        }
    }
}
