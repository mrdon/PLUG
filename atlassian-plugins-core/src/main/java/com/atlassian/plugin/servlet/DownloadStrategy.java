package com.atlassian.plugin.servlet;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public interface DownloadStrategy
{
    boolean matches(String urlPath);
    void serveFile(BaseFileServerServlet servlet, HttpServletRequest req, HttpServletResponse resp) throws IOException;
}