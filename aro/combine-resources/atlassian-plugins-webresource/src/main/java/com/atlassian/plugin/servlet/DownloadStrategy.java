package com.atlassian.plugin.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface DownloadStrategy
{
    /**
     * Returns true if the DownloadStrategy is supported for the given url path.
     */
    boolean matches(String urlPath);

    /**
     * Serves the file for the given request and response.
     * @throws DownloadException if there was an error during serving of the file.
     */
    void serveFile(HttpServletRequest req, HttpServletResponse resp) throws DownloadException;
}