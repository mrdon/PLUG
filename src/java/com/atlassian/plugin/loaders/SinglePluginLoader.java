package com.atlassian.plugin.loaders;

import com.atlassian.core.util.ClassLoaderUtils;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.net.URL;
import java.util.*;

public class SinglePluginLoader implements PluginLoader
{
    private static Log log = LogFactory.getLog(SinglePluginLoader.class);

    List plugins;
    private URL url;

    public SinglePluginLoader(String resource) throws PluginParseException
    {
        this.url = ClassLoaderUtils.getResource(resource, SinglePluginLoader.class);

        if (url == null)
        {
            throw new PluginParseException("Cannot load plugins from bad resource : " + resource);
        }
    }

    public SinglePluginLoader(URL url)
    {
        this.url = url;
    }

    private void loadPlugins(Map moduleDescriptors) throws PluginParseException
    {
        if (url == null)
            throw new PluginParseException("Cannot load plugins from null URL : " + url);

        Plugin plugin = new Plugin();

        SAXReader reader = new SAXReader();
        try
        {
            Document doc = reader.read(url);
            Element root = doc.getRootElement();

            plugin.setName(root.attributeValue("name"));
            plugin.setKey(root.attributeValue("key"));

            if ("disabled".equalsIgnoreCase(root.attributeValue("state")))
                plugin.setEnabledByDefault(false);

            for (Iterator i = root.elementIterator(); i.hasNext();)
            {
                Element element = (Element) i.next();

                if ("description".equalsIgnoreCase(element.getName()))
                {
                    plugin.setDescription(element.getTextTrim());
                }
                else
                {
                    ModuleDescriptor moduleDescriptor = createModuleDescriptor(element, moduleDescriptors);

                    if (moduleDescriptor != null)
                        plugin.addModule(moduleDescriptor);
                }
            }
        }
        catch (DocumentException e)
        {
            throw new PluginParseException("Exception parsing plugin document: " + url, e);
        }

        plugins.add(plugin);
    }

    public Collection getPlugins(Map moduleDescriptors)
    {
        if (plugins == null)
        {
            try
            {
                plugins = new ArrayList();
                loadPlugins(moduleDescriptors);
            }
            catch (PluginParseException e)
            {
                log.error("Could not load plugins for : " + url, e);
            }
        }

        return plugins;
    }

    public ModuleDescriptor createModuleDescriptor(Element element, Map moduleDescriptors) throws PluginParseException
    {
        String name = element.getName();

        Class descriptorClass = (Class) moduleDescriptors.get(name);

        if (descriptorClass == null)
        {
            throw new PluginParseException("Could not find interpreter for module: " + name);
        }

        ModuleDescriptor moduleDescriptorDescriptor = null;

        try
        {
            moduleDescriptorDescriptor = (ModuleDescriptor) ClassLoaderUtils.loadClass(descriptorClass.getName(), SinglePluginLoader.class).newInstance();
        }
        catch (InstantiationException e)
        {
            throw new PluginParseException("Could not instantiate module descriptor: " + descriptorClass.getName(), e);
        }
        catch (IllegalAccessException e)
        {
            throw new PluginParseException("Exception instantiating module descriptor: " + descriptorClass.getName(), e);
        }
        catch (ClassNotFoundException e)
        {
            throw new PluginParseException("Could not find module descriptor class: " + descriptorClass.getName(), e);
        }

        moduleDescriptorDescriptor.init(element);

        return moduleDescriptorDescriptor;
    }

}
