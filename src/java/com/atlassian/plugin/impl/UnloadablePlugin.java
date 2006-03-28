package com.atlassian.plugin.impl;

/**
 * This class represents a Plugin that was not able to be loaded by the PluginManager.
 *
 * @see com.atlassian.plugin.DefaultPluginManager
 * @see com.atlassian.plugin.loaders.AbstractXmlPluginLoader
 */
public class UnloadablePlugin extends StaticPlugin
{
    private String errorText;

    public boolean isEnabledByDefault()
    {
        return false;
    }

    public String getErrorText()
    {
        return errorText;
    }

    public void setErrorText(String errorText)
    {
        this.errorText = errorText;
    }
}
