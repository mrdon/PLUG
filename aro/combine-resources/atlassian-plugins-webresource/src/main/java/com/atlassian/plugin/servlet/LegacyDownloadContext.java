package com.atlassian.plugin.servlet;

/**
 * A legacy implementation of {@link ApplicationDownloadContext} to replace {@link BaseFileServerServlet}
 * from being passed around.
 */
public class LegacyDownloadContext implements ApplicationDownloadContext
{
    private BaseFileServerServlet servlet;

    public LegacyDownloadContext(BaseFileServerServlet servlet)
    {
        this.servlet = servlet;
    }

    public String getContentType(String path)
    {
        return servlet.getContentType(path);
    }

    public String getCharacterEncoding()
    {
        return null;
    }
}
