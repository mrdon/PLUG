package com.atlassian.plugin.servlet;

/**
 * An interface to encapsulate the application's download context.
 */
public interface ApplicationDownloadContext
{
    /**
     * Returns the content type for the given resource path.
     */
    String getContentType(String path);

    /**
     * Returns the application's character encoding.
     */
    String getCharacterEncoding();
}
