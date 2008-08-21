package com.atlassian.plugin.refimpl.servlet;

import com.atlassian.plugin.refimpl.ContainerManager;
import com.atlassian.plugin.servlet.AbstractFileServerServlet;
import com.atlassian.plugin.servlet.DownloadStrategy;

import java.util.List;

public class FileServerServlet extends AbstractFileServerServlet
{
    private List downloadStrategies;

    protected List<DownloadStrategy> getDownloadStrategies()
    {
        if(downloadStrategies == null)
            downloadStrategies = ContainerManager.getInstance().getDownloadStrategies();

        return downloadStrategies;
    }
}
