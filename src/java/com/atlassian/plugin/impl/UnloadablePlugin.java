package com.atlassian.plugin.impl;

/**
 * This class represents a Plugin that was not able to be loaded by the PluginManager.
 *
 * @see com.atlassian.plugin.DefaultPluginManager
 */
public class UnloadablePlugin extends StaticPlugin
{
    private String errorText;
    private boolean uninstallable;
    private boolean deletable;

    public boolean isUninstallable()
    {
        return uninstallable;
    }

    public void setDeletable(boolean deletable)
    {
        this.deletable = deletable;
    }

    public boolean isDeleteable()
    {
        return deletable;
    }

    public void setUninstallable(boolean uninstallable)
    {
        this.uninstallable = uninstallable;
    }

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
