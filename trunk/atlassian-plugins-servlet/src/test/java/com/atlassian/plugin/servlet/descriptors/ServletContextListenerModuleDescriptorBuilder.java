package com.atlassian.plugin.servlet.descriptors;

import javax.servlet.ServletContextListener;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.servlet.PluginBuilder;
import com.atlassian.plugin.servlet.ServletModuleManager;

public class ServletContextListenerModuleDescriptorBuilder
{
    private Plugin plugin = new PluginBuilder().build();
    private String key = "test.servlet.context.listener";
    private ServletContextListener listener;

    public ServletContextListenerModuleDescriptorBuilder with(Plugin plugin)
    {
        this.plugin = plugin;
        return this;
    }
    
    public ServletContextListenerModuleDescriptorBuilder withKey(String key)
    {
        this.key = key;
        return this;
    }

    public ServletContextListenerModuleDescriptorBuilder with(ServletContextListener listener)
    {
        this.listener = listener;
        return this;
    }
    
    public ServletContextListenerModuleDescriptor build()
    {
        Descriptor d = new Descriptor(plugin, key, listener);
        plugin.addModuleDescriptor(d);
        return d;
    }

    private static final class Descriptor extends ServletContextListenerModuleDescriptor
    {
        final Plugin plugin;
        final String key;
        final ServletContextListener listener; 
        
        public Descriptor(
            Plugin plugin,
            String key,
            ServletContextListener listener)
        {
            super(new DefaultHostContainer());
            this.plugin = plugin;
            this.key = key;
            this.listener = listener;
        }

        @Override
        public Plugin getPlugin()
        {
            return plugin;
        }
        
        @Override
        public String getCompleteKey()
        {
            return getPluginKey() + ":" + key;
        }

        @Override
        public String getKey()
        {
            return key;
        }

        @Override
        public ServletContextListener getModule()
        {
            return listener;
        }
    }
}
