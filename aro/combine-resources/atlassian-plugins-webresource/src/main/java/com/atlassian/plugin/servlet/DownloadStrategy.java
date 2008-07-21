package com.atlassian.plugin.servlet;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public interface DownloadStrategy
{
    /**
     * Returns true if the DownloadStrategy is supported for the given url path.
     */
    boolean matches(String urlPath);

    /**
     * Serves the file for the request in the given application context.
     */
    void serveFile(HttpServletRequest req, HttpServletResponse resp, ApplicationDownloadContext context) throws IOException;

    /**
     * @deprecated Since 2.0. Use {@link #serveFile(HttpServletRequest, HttpServletResponse, ApplicationDownloadContext)} instead.
     */
    void serveFile(BaseFileServerServlet servlet, HttpServletRequest req, HttpServletResponse resp) throws IOException;
}