package com.atlassian.plugin.loaders;

import com.atlassian.plugin.*;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptor;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptorFactory;
import com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptorFactory;
import com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor;
import com.atlassian.plugin.impl.UnloadablePluginFactory;
import com.atlassian.plugin.util.ClassLoaderUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
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

        ModuleDescriptor moduleDescriptorDescriptor = null;

        // Try to retrieve the module descriptor
        try
        {
            moduleDescriptorDescriptor = moduleDescriptorFactory.getModuleDescriptor(name);
        }
        // When there's a problem loading a module, return an UnrecognisedModuleDescriptor with error
        catch (Exception e)
        {
            UnrecognisedModuleDescriptor descriptor = UnrecognisedModuleDescriptorFactory.createUnrecognisedModuleDescriptor(plugin, element, e, moduleDescriptorFactory);

            log.error("There were problems loading the module '" + name + "' in plugin '" + plugin.getName() + "'. The module has been disabled.");
            log.error(descriptor.getErrorText(), e);

            return descriptor;
        }

        // When the module descriptor has been excluded, null is returned (PLUG-5)
        if (moduleDescriptorDescriptor == null)
        {
            log.info("The module '" + name + "' in plugin '" + plugin.getName() + "' is in the list of excluded module descriptors, so not enabling.");
            return null;
        }

        // Once we have the module descriptor, create it using the given information
        try
        {
            moduleDescriptorDescriptor.init(plugin, element);
        }
        // If it fails, return a dummy module that contains the error
        catch (PluginParseException e)
        {
            UnloadableModuleDescriptor descriptor = UnloadableModuleDescriptorFactory.createUnloadableModuleDescriptor(plugin, element, e, moduleDescriptorFactory);

            log.error("There were problems loading the module '" + name + "'. The module and its plugin have been disabled.");
            log.error(descriptor.getErrorText(), e);

            return descriptor;
        }

        return moduleDescriptorDescriptor;
    }

    protected PluginInformation createPluginInformation(Element element)
    {
        PluginInformation pluginInfo = new PluginInformation();

        if (element.element("description") != null)
        {
            pluginInfo.setDescription(element.element("description").getTextTrim());
            if(element.element("description").attributeValue("key") != null)
            {
                pluginInfo.setDescriptionKey(element.element("description").attributeValue("key"));
            }
        }

        if (element.element("version") != null)
            pluginInfo.setVersion(element.element("version").getTextTrim());

        if (element.element("vendor") != null)
        {
            final Element vendor = element.element("vendor");
            pluginInfo.setVendorName(vendor.attributeValue("name"));
            pluginInfo.setVendorUrl(vendor.attributeValue("url"));
        }

        // initialize any parameters on the plugin xml definition
        for (Iterator iterator = element.elements("param").iterator(); iterator.hasNext();)
        {
            Element param = (Element) iterator.next();

            // Retrieve the parameter info => name & text
            if (param.attribute("name") != null)
                pluginInfo.addParameter(param.attribute("name").getData().toString(), param.getText());
        }

        if (element.element("application-version") != null)
        {
            pluginInfo.setMaxVersion(Float.parseFloat(element.element("application-version").attributeValue("max")));
            pluginInfo.setMinVersion(Float.parseFloat(element.element("application-version").attributeValue("min")));
        }

        if (element.element("java-version") != null)
        {
            pluginInfo.setMinJavaVersion(Float.valueOf(element.element("java-version").attributeValue("min")));
        }

        return pluginInfo;
    }

    /**
     * Configures a plugin and the modules it contains
     *
     * Returns an UnloadablePlugin instance when there were errors loading any modules
     *
     * @param moduleDescriptorFactory
     * @param doc
     * @param plugin
     * @return
     * @throws PluginParseException
     */
    protected Plugin configurePlugin(ModuleDescriptorFactory moduleDescriptorFactory, Document doc, Plugin plugin) throws PluginParseException
    {
        Element root = doc.getRootElement();

        plugin.setName(root.attributeValue("name"));
        plugin.setKey(root.attributeValue("key"));

        if(root.attributeValue("i18n-name-key") != null)
        {
            plugin.setI18nNameKey(root.attributeValue("i18n-name-key"));
        }

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

                // If we're not loading the module descriptor, null is returned, so we skip it
                if (moduleDescriptor == null)
                    continue;

                if (plugin.getModuleDescriptor(moduleDescriptor.getKey()) != null)
                    throw new PluginParseException("Found duplicate key '" + moduleDescriptor.getKey() + "' within plugin '" + plugin.getKey() + "'");

                plugin.addModuleDescriptor(moduleDescriptor);

                // If we have any unloadable modules, also create an unloadable plugin, which will make it clear that there was a problem
                if (moduleDescriptor instanceof UnloadableModuleDescriptor)
                {
                    log.error("There were errors loading the plugin '" + plugin.getName() + "'. The plugin has been disabled.");
                    return UnloadablePluginFactory.createUnloadablePlugin(plugin);
                }
            }
        }

        plugin.setResources(Resources.fromXml(root));

        return plugin;
    }
}
