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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.util.*;
import java.io.File;
import java.io.InputStream;

public class SinglePluginLoader implements PluginLoader
{
    private static Log log = LogFactory.getLog(SinglePluginLoader.class);

    List plugins;
    private URL url;
    private String resource;

    public SinglePluginLoader(String resource) throws PluginParseException
    {
        this.resource = resource;
    }

    public SinglePluginLoader(URL url)
    {
        this.url = url;
    }

    private void loadPlugins(Map moduleDescriptors) throws PluginParseException
    {
        if (url == null && resource == null)
            throw new PluginParseException("No resource or URL specified to load plugins from.");

        Plugin plugin = new Plugin();

        try
        {
            Document doc = getDocument();
            Element root = doc.getRootElement();

            plugin.setName(root.attributeValue("name"));
            plugin.setKey(root.attributeValue("key"));

            if (plugin.getKey().indexOf(":") > 0)
                throw new PluginParseException("Plugin key's cannot contain ':'. Key is '" + plugin.getKey() + "'");

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
                    ModuleDescriptor moduleDescriptor = createModuleDescriptor(plugin, element, moduleDescriptors);

                    if (plugin.getModule(moduleDescriptor.getKey()) != null)
                        throw new PluginParseException("Found duplicate key '" + moduleDescriptor.getKey() + "' within plugin '" + plugin.getKey() + "'");

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

    private Document getDocument() throws DocumentException, PluginParseException {
        SAXReader reader = new SAXReader();

        if (resource != null)
        {
            final InputStream is = ClassLoaderUtils.getResourceAsStream(resource, SinglePluginLoader.class);

            if (is == null)
                throw new PluginParseException("Couldn't find resource: " + resource);

            return reader.read(is);
        }
        else
            return reader.read(url);
    }

    public Collection getPlugins(Map moduleDescriptors) throws PluginParseException
    {
        if (plugins == null)
        {
            plugins = new ArrayList();
            loadPlugins(moduleDescriptors);
        }

        return plugins;
    }

    private ModuleDescriptor createModuleDescriptor(Plugin plugin, Element element, Map moduleDescriptors) throws PluginParseException
    {
        String name = element.getName();

        Class descriptorClass = (Class) moduleDescriptors.get(name);

        if (descriptorClass == null)
        {
            throw new PluginParseException("Could not find descriptor for module: " + name);
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

        moduleDescriptorDescriptor.init(plugin, element);

        return moduleDescriptorDescriptor;
    }

}
