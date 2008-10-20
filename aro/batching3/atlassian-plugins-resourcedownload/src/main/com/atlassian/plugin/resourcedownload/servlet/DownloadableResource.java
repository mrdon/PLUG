package com.atlassian.plugin.resourcedownload.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface DownloadableResource
{
    /**
     * Checks any "If-Modified-Since" header from the request against the plugin's loading time, since plugins can't
     * be modified after they've been loaded this is a good way to determine if a plugin resource has been modified
     * or not.
     *
     * If this method returns true, don't do any more processing on the request -- the response code has already been
     * set to "304 Not Modified" for you, and you don't need to serve the file.
     */
    boolean checkResourceNotModified(HttpServletRequest request, HttpServletResponse response);

    void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException;
}
