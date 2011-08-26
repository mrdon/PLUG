package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.impl.StaticPlugin;

public class PluginBuilder
{
    private String key = "test.plugin";

    public PluginBuilder withKey(String key)
    {
        this.key = key;
        return this;
    }
    
    public Plugin build()
    {
        StaticPlugin plugin = new StaticPlugin();
        plugin.setKey(key);
        return plugin;
    }
}
