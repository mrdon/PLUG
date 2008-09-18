package com.atlassian.plugin.servlet.descriptors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.servlet.PluginBuilder;
import com.atlassian.plugin.servlet.ServletModuleManager;

public class ServletModuleDescriptorBuilder
{
    private Plugin plugin = new PluginBuilder().build();
    private String key = "test.servlet";
    private HttpServlet servlet;
    private List<String> paths = new LinkedList<String>();
    private ServletModuleManager servletModuleManager;
    private Map<String, String> initParams = new HashMap<String, String>();
    
    public ServletModuleDescriptorBuilder with(Plugin plugin)
    {
        this.plugin = plugin;
        return this;
    }
    
    public ServletModuleDescriptorBuilder withKey(String key)
    {
        this.key = key;
        return this;
    }

    public ServletModuleDescriptorBuilder with(HttpServlet servlet)
    {
        this.servlet = servlet;
        return this;
    }

    public ServletModuleDescriptorBuilder withPath(String path)
    {
        paths.add(path);
        return this;
    }

    public ServletModuleDescriptorBuilder with(ServletModuleManager servletModuleManager)
    {
        this.servletModuleManager = servletModuleManager;
        return this;
    }
    
    public ServletModuleDescriptorBuilder withInitParam(String name, String value)
    {
        initParams.put(name, value);
        return this;
    }

    public ServletModuleDescriptor build()
    {
        Descriptor d = new Descriptor(plugin, key, servlet, immutableList(paths), immutableMap(initParams), servletModuleManager);
        plugin.addModuleDescriptor(d);
        return d;
    }
    
    static <K, V> Map<K, V> immutableMap(Map<K, V> initParams)
    {
        return Collections.unmodifiableMap(new HashMap<K, V>(initParams));
    }

    <T> List<T> immutableList(List<T> list)
    {
        List<T> copy = new ArrayList<T>(list.size());
        copy.addAll(list);
        return Collections.unmodifiableList(copy);
    }
    
    static final class Descriptor extends ServletModuleDescriptor
    {
        final String key;
        final HttpServlet servlet; 
        final List<String> paths;
        final ServletModuleManager servletModuleManager;
        final Map<String, String> initParams;
        
        public Descriptor(
            Plugin plugin,
            String key,
            HttpServlet servlet,
            List<String> paths,
            Map<String, String> initParams,
            ServletModuleManager servletModuleManager)
        {
            this.plugin = plugin;
            this.key = key;
            this.servlet = servlet;
            this.paths = paths;
            this.initParams = initParams;
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
        public HttpServlet getModule()
        {
            return servlet;
        }


        @Override
        public List<String> getPaths()
        {
            return paths;
        }
        
        @Override
        public Map<String, String> getInitParams()
        {
            return initParams;
        }

        @Override
        protected ServletModuleManager getServletModuleManager()
        {
            return servletModuleManager;
        }
    }

}
