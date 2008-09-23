package com.atlassian.plugin.refimpl;

public class ParameterUtils
{
    public static String getBaseUrl()
    {
        return System.getProperty("baseurl", "http://localhost:8080/atlassian-plugins-refimpl");
    }
}
