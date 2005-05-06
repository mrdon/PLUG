package com.atlassian.plugin.loaders;

import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.atlassian.plugin.*;
import com.atlassian.plugin.util.ClassLoaderUtils;

import java.io.InputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public abstract class AbstractXmlPluginLoader implements PluginLoader
{
    private static Log log = LogFactory.getLog(AbstractXmlPluginLoader.class);
    protected boolean recogniseSystemPlugins = false;

    protected boolean isResource(Element element)
    {
        return "resource".equalsIgnoreCase(element.getName());
    }

    protected Document getDocument(String resource) throws DocumentException, PluginParseException
    {
        if (resource == null)
            throw new PluginParseException("Couldn't find resource: " + resource);

        final InputStream is = ClassLoaderUtils.getResourceAsStream(resource, AbstractXmlPluginLoader.class);
        return getDocument(is);
    }

    protected Document getDocument(InputStream is) throws DocumentException, PluginParseException
    {
        if (is == null)
            throw new PluginParseException("Invalid InputStream specified?");

        SAXReader reader = new SAXReader();

        try
        {
            return reader.read(is);
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (IOException e)
            {
                log.error("Bad inputstream close: " + e, e);
            }
        }
    }

    public abstract Collection loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException;

    protected ModuleDescriptor createModuleDescriptor(Plugin plugin, Element element, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        String name = element.getName();

        if (!moduleDescriptorFactory.hasModuleDescriptor(name))
        {
            throw new PluginParseException("Could not find descriptor for module '" + name +"' in plugin '" + (plugin == null ? "null" : plugin.getName()) + "'");
        }

        ModuleDescriptor moduleDescriptorDescriptor = null;

        try
        {
            moduleDescriptorDescriptor = moduleDescriptorFactory.getModuleDescriptor(name);
        }
        catch (InstantiationException e)
        {
            throw new PluginParseException("Could not instantiate module descriptor: " + moduleDescriptorFactory.getModuleDescriptorClass(name).getName(), e);
        }
        catch (IllegalAccessException e)
        {
            throw new PluginParseException("Exception instantiating module descriptor: " + moduleDescriptorFactory.getModuleDescriptorClass(name).getName(), e);
        }
        catch (ClassNotFoundException e)
        {
            throw new PluginParseException("Could not find module descriptor class: " + moduleDescriptorFactory.getModuleDescriptorClass(name).getName(), e);
        }

        moduleDescriptorDescriptor.init(plugin, element);

        return moduleDescriptorDescriptor;
    }

    protected PluginInformation createPluginInformation(Element element)
    {
        PluginInformation pluginInfo = new PluginInformation();

        if (element.element("description") != null)
            pluginInfo.setDescription(element.element("description").getTextTrim());

        if (element.element("version") != null)
            pluginInfo.setVersion(element.element("version").getTextTrim());

        if (element.element("vendor") != null)
        {
            final Element vendor = element.element("vendor");
            pluginInfo.setVendorName(vendor.attributeValue("name"));
            pluginInfo.setVendorUrl(vendor.attributeValue("url"));
        }

        if (element.element("application-version") != null)
        {
            pluginInfo.setMaxVersion(Float.parseFloat(element.element("application-version").attributeValue("max")));
            pluginInfo.setMinVersion(Float.parseFloat(element.element("application-version").attributeValue("min")));
        }

        return pluginInfo;
    }

    protected Plugin configurePlugin(ModuleDescriptorFactory moduleDescriptorFactory, Document doc, Plugin plugin) throws PluginParseException
    {
        Element root = doc.getRootElement();

        plugin.setName(root.attributeValue("name"));
        plugin.setKey(root.attributeValue("key"));

        if (plugin.getKey().indexOf(":") > 0)
            throw new PluginParseException("Plugin key's cannot contain ':'. Key is '" + plugin.getKey() + "'");

        if ("disabled".equalsIgnoreCase(root.attributeValue("state")))
            plugin.setEnabledByDefault(false);

        if (recogniseSystemPlugins && "true".equalsIgnoreCase(root.attributeValue("system")))
            plugin.setSystemPlugin(true);

        for (Iterator i = root.elementIterator(); i.hasNext();)
        {
            Element element = (Element) i.next();

            if ("plugin-info".equalsIgnoreCase(element.getName()))
            {
                plugin.setPluginInformation(createPluginInformation(element));
            }
            else if (!isResource(element))
            {
                ModuleDescriptor moduleDescriptor = createModuleDescriptor(plugin, element, moduleDescriptorFactory);

                if (plugin.getModuleDescriptor(moduleDescriptor.getKey()) != null)
                    throw new PluginParseException("Found duplicate key '" + moduleDescriptor.getKey() + "' within plugin '" + plugin.getKey() + "'");

                if (moduleDescriptor != null)
                    plugin.addModuleDescriptor(moduleDescriptor);
            }
        }

        plugin.setResources(Resources.fromXml(root));

        return plugin;
    }
}
