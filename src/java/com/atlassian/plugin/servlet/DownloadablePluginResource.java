package com.atlassian.plugin.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface DownloadablePluginResource
{
    void serveResource(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException;
}
