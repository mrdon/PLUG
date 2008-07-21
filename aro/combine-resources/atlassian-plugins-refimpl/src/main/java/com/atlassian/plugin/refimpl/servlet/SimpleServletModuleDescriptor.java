package com.atlassian.plugin.refimpl.servlet;

import com.atlassian.plugin.descriptors.servlet.ServletModuleDescriptor;
import com.atlassian.plugin.descriptors.servlet.ServletModuleManager;
import com.atlassian.plugin.refimpl.ContainerManager;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: 06/07/2008
 * Time: 1:08:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleServletModuleDescriptor extends ServletModuleDescriptor {
    protected void autowireObject(Object o) {
        throw new UnsupportedOperationException("Only 2.0 plugins are supported");
    }

    protected ServletModuleManager getServletModuleManager() {
        return ContainerManager.getInstance().getServletModuleManager();
    }
}
