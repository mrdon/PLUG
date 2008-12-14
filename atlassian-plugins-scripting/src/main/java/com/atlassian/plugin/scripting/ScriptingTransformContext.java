package com.atlassian.plugin.scripting;

import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

import java.util.List;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: Dec 14, 2008
 * Time: 10:22:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScriptingTransformContext extends TransformContext
{
    private final ScriptManager scriptManager;
    public ScriptingTransformContext(List<HostComponentRegistration> regs, File pluginFile, String descriptorPath, ScriptManager scriptManager)
    {
        super(regs, pluginFile, descriptorPath);
        this.scriptManager = scriptManager;
    }

    public ScriptManager getScriptManager()
    {
        return scriptManager;
    }
}
