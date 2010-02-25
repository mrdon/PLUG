package com.atlassian.plugin.servlet.descriptors;

import javax.servlet.ServletContextListener;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.module.ClassModuleCreator;
import com.atlassian.plugin.module.DefaultModuleClassFactory;
import com.atlassian.plugin.module.ModuleClassFactory;
import com.atlassian.plugin.module.ModuleCreator;
import com.atlassian.plugin.servlet.PluginBuilder;

import java.util.ArrayList;
import java.util.List;

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
        final List<ModuleCreator> provider = new ArrayList<ModuleCreator>();
        provider.add(new ClassModuleCreator(new DefaultHostContainer()));
        DefaultModuleClassFactory defaultModuleClassFactory = new DefaultModuleClassFactory(provider);
        Descriptor d = new Descriptor(plugin, key, listener, defaultModuleClassFactory);
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
            ServletContextListener listener, ModuleClassFactory moduleClassFactory)
        {
            super(moduleClassFactory);
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
