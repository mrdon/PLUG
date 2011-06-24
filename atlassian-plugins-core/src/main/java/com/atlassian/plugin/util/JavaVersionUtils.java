package com.atlassian.plugin.util;

/**
 * A simple utility for comparing a java version number to the running version.
 */
public class JavaVersionUtils
{
    private static final Float SPEC_VERSION = Float.valueOf(System.getProperty("java.specification.version"));

    public static boolean satisfiesMinVersion(float versionNumber)
    {
        return SPEC_VERSION >= versionNumber;
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
