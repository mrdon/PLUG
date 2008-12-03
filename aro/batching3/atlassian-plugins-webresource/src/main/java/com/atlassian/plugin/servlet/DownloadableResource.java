package com.atlassian.plugin.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Represents a plugin resource that can be downloaded.
 *
 * It is up to the calling class to check if the resource is modified before calling
 * {@link #serveResource(HttpServletRequest, HttpServletResponse)} to serve the resource.
 */
public interface DownloadableResource
{
    /**
     * Returns true if the plugin resource has been modified. The implementing class is responsible for
     * setting any appropriate response codes or headers on the response.
     *
     * If the resource has been modified, the resource shouldn't be served. 
     */
    boolean isResourceModified(HttpServletRequest request, HttpServletResponse response);

    /**
     * Writes the resource content out into the response.
     * @throws DownloadException if there were errors writing to the response.
     */
    void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException;

    /**
     * Returns the content type for the resource. May return null if it cannot resolve its own content type.
     */
    String getContentType();
}
