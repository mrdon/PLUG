package com.atlassian.plugin.refimpl.servlet;

import com.atlassian.plugin.refimpl.ContainerManager;
import com.atlassian.plugin.servlet.ServletModuleContainerServlet;
import com.atlassian.plugin.servlet.ServletModuleManager;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: 06/07/2008
 * Time: 12:17:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleContainerServlet extends ServletModuleContainerServlet {
    
    protected ServletModuleManager getServletModuleManager() {
        return ContainerManager.getInstance().getServletModuleManager();
    }
}
