package com.atlassian.plugin.loaders;

import com.atlassian.core.util.ClassLoaderUtils;
import com.atlassian.plugin.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SinglePluginLoader implements PluginLoader
{
    private static Log log = LogFactory.getLog(SinglePluginLoader.class);

    List plugins;
    private String resource;
    private InputStream is;

    public SinglePluginLoader(String resource)
    {
        this.resource = resource;
    }

    public SinglePluginLoader(InputStream is)
    {
        this.is = is;
    }

    private void loadPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        if (resource == null && is == null)
            throw new PluginParseException("No resource or inputstream specified to load plugins from.");

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

            plugin.setResourceDescriptors(LoaderUtils.getResourceDescriptors(root));
        }
        catch (DocumentException e)
        {
            throw new PluginParseException("Exception parsing plugin document", e);
        }

        plugins.add(plugin);
    }

    private boolean isResource(Element element)
    {
        return "resource".equalsIgnoreCase(element.getName());
    }

    private Document getDocument() throws DocumentException, PluginParseException
    {
        SAXReader reader = new SAXReader();

        if (resource != null)
        {
            final InputStream is = ClassLoaderUtils.getResourceAsStream(resource, SinglePluginLoader.class);

            if (is == null)
                throw new PluginParseException("Couldn't find resource: " + resource);

            return reader.read(is, resource);
        }
        else if (is != null)
        {
            try
            {
                return reader.read(is, resource);
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
        else
            throw new PluginParseException("No resource or input stream specified.");
    }

    public Collection getPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        if (plugins == null)
        {
            plugins = new ArrayList();
            loadPlugins(moduleDescriptorFactory);
        }

        return plugins;
    }

    private ModuleDescriptor createModuleDescriptor(Plugin plugin, Element element, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
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

    private PluginInformation createPluginInformation(Element element)
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

        if (element.element("license-registry-location") != null)
        {
            pluginInfo.setLicenseRegistryLocation(element.element("license-registry-location").getTextTrim());
        }

        if (element.element("license-store-location") != null)
        {
            pluginInfo.setLicenseTypeStoreLocation(element.element("license-store-location").getTextTrim());
        }

        return pluginInfo;
    }

}
