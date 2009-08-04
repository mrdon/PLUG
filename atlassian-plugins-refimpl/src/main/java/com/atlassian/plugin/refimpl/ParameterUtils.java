package com.atlassian.plugin.refimpl;

import java.net.URI;

import com.atlassian.plugin.webresource.UrlMode;

public class ParameterUtils
{
    public static String getBaseUrl(UrlMode urlMode)
    {
        String port = System.getProperty("http.port", "8080");
        String baseUrl = System.getProperty("baseurl", "http://localhost:" + port + "/atlassian-plugins-refimpl");
        if (urlMode == UrlMode.ABSOLUTE)
        {
            return baseUrl;
        }
        return URI.create(baseUrl).getPath();
    }
}
