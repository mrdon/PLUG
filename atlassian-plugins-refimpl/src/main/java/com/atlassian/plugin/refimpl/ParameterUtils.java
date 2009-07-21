package com.atlassian.plugin.refimpl;

import java.net.URI;

import com.atlassian.plugin.webresource.WebResourceManager;

public class ParameterUtils
{
    public static String getBaseUrl(WebResourceManager.UrlMode urlMode)
    {
        String port = System.getProperty("http.port", "8080");
        String baseUrl = System.getProperty("baseurl", "http://localhost:" + port + "/atlassian-plugins-refimpl");
        if (urlMode == WebResourceManager.UrlMode.ABSOLUTE)
        {
            return baseUrl;
        }
        return URI.create(baseUrl).getPath();
    }
}
