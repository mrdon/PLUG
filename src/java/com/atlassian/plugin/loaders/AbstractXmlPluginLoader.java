package com.atlassian.plugin.loaders;

import com.atlassian.plugin.*;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptor;
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
import java.util.ArrayList;
import java.util.List;

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
        // When there's a problem loading a module, note the problem and provide a dummy module that can report the error
        catch (Exception e)
        {
            UnloadableModuleDescriptor descriptor = new UnloadableModuleDescriptor();
            descriptor.init(plugin, element);

            String errorMsg = null;

            if (e instanceof PluginParseException)
                errorMsg = "Could not find descriptor for module '" + name + "' in plugin '" + (plugin == null ? "null" : plugin.getName()) + "'";
            else if (e instanceof InstantiationException)
                errorMsg = "Could not instantiate module descriptor: " + moduleDescriptorFactory.getModuleDescriptorClass(name).getName();
            else if (e instanceof IllegalAccessException)
                errorMsg = "Exception instantiating module descriptor: " + moduleDescriptorFactory.getModuleDescriptorClass(name).getName();
            else if (e instanceof ClassNotFoundException)
                errorMsg = "Could not find module descriptor class: " + moduleDescriptorFactory.getModuleDescriptorClass(name).getName();
            else
                errorMsg = "There was a problem loading the module descriptor: " + moduleDescriptorFactory.getModuleDescriptorClass(name).getName();

            log.error("There were problems loading the module '" + name + "'. The module and its plugin have been disabled.");
            log.error(errorMsg, e);

            descriptor.setErrorText(errorMsg);

            return descriptor;
        }

        // Once we have the module descriptor, create it using the given information
        try
        {
            moduleDescriptorDescriptor.init(plugin, element);
        }
        // If it fails, return a dummy module that contains the error
        catch (PluginParseException e)
        {
            UnloadableModuleDescriptor descriptor = new UnloadableModuleDescriptor();
            descriptor.init(plugin, element);

            descriptor.setErrorText(e.getMessage());

            log.error("There were problems loading the module '" + name + "'. The module and its plugin have been disabled.");
            log.error(e);

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

                if (plugin.getModuleDescriptor(moduleDescriptor.getKey()) != null)
                    throw new PluginParseException("Found duplicate key '" + moduleDescriptor.getKey() + "' within plugin '" + plugin.getKey() + "'");

                plugin.addModuleDescriptor(moduleDescriptor);

                // If we have any unloadable modules, also create an unloadable plugin, which will make it clear that there was a problem
                if (moduleDescriptor instanceof UnloadableModuleDescriptor)
                {
                    log.error("There were errors loading the plugin '" + plugin.getName() + "'. The plugin has been disabled.");
                    return createUnloadablePlugin(plugin);
                }
            }
        }

        plugin.setResources(Resources.fromXml(root));

        return plugin;
    }

    /**
     * Creates an UnloadablePlugin instance from a given plugin, when there were problems loading the modules or the plugin itself
     *
     * @param oldPlugin
     * @return UnloadablePlugin instance
     */
    private UnloadablePlugin createUnloadablePlugin(Plugin oldPlugin)
    {
        UnloadablePlugin newPlugin = new UnloadablePlugin();

        newPlugin.setName(oldPlugin.getName());
        newPlugin.setKey(oldPlugin.getKey());
        newPlugin.setI18nNameKey(oldPlugin.getI18nNameKey());

        // Make sure it's visible to the user
        newPlugin.setSystemPlugin(false);

        newPlugin.setPluginInformation(oldPlugin.getPluginInformation());

        List moduleDescriptors = new ArrayList(oldPlugin.getModuleDescriptors());

        for (int i = 0; i < moduleDescriptors.size(); i++)
        {
            ModuleDescriptor descriptor = (ModuleDescriptor) moduleDescriptors.get(i);

            newPlugin.addModuleDescriptor(descriptor);
        }

        return newPlugin;
    }
}
