package com.atlassian.plugin.servlet.descriptors;

import static com.atlassian.plugin.servlet.filter.FilterTestUtils.immutableList;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.Filter;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.servlet.PluginBuilder;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.filter.FilterLocation;

public class ServletFilterModuleDescriptorBuilder
{
    private Plugin plugin = new PluginBuilder().build();
    private String key = "test.servlet.context.listener";
    private Filter filter;
    private FilterLocation location = FilterLocation.BEFORE_DISPATCH;
    private int weight = 100;
    private List<String> paths = new LinkedList<String>();
    private ServletModuleManager servletModuleManager;
    
    public ServletFilterModuleDescriptorBuilder with(Plugin plugin)
    {
        this.plugin = plugin;
        return this;
    }
    
    public ServletFilterModuleDescriptorBuilder withKey(String key)
    {
        this.key = key;
        return this;
    }

    public ServletFilterModuleDescriptorBuilder with(Filter filter)
    {
        this.filter = filter;
        return this;
    }

    public ServletFilterModuleDescriptorBuilder withPath(String path)
    {
        paths.add(path);
        return this;
    }

    public ServletFilterModuleDescriptorBuilder with(ServletModuleManager servletModuleManager)
    {
        this.servletModuleManager = servletModuleManager;
        return this;
    }
    
    public ServletFilterModuleDescriptorBuilder at(FilterLocation location)
    {
        this.location = location;
        return this;
    }
    
    public ServletFilterModuleDescriptorBuilder withWeight(int weight)
    {
        this.weight = weight;
        return this;
    }

    public ServletFilterModuleDescriptor build()
    {
        return new Descriptor(plugin, key, filter, location, weight, immutableList(paths), servletModuleManager);
    }
    
    static final class Descriptor extends ServletFilterModuleDescriptor
    {
        final String key;
        final Filter filter; 
        final List<String> paths;
        final FilterLocation location;
        final int weight;
        final ServletModuleManager servletModuleManager;
        
        public Descriptor(
            Plugin plugin,
            String key,
            Filter filter,
            FilterLocation location,
            int weight,
            List<String> paths,
            ServletModuleManager servletModuleManager)
        {
            this.plugin = plugin;
            this.key = key;
            this.filter = filter;
            this.location = location;
            this.weight = weight;
            this.paths = paths;
            this.servletModuleManager = servletModuleManager;
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
        protected void autowireObject(Object obj) {}

        @Override
        public Filter getModule()
        {
            return filter;
        }
        
        @Override
        public FilterLocation getLocation()
        {
            return location;
        }
        
        @Override
        public int getWeight()
        {
            return weight;
        }

        @Override
        public List<String> getPaths()
        {
            return paths;
        }

        @Override
        protected ServletModuleManager getServletModuleManager()
        {
            return servletModuleManager;
        }
    }

}