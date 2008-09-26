package com.atlassian.plugin.refimpl;

public class ParameterUtils
{
    public static String getBaseUrl()
    {
        String port = System.getProperty("http.port", "8080");
        return System.getProperty("baseurl", "http://localhost:" + port + "/atlassian-plugins-refimpl");
    }
}
