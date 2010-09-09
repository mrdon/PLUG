package com.atlassian.labs.plugins3.impl;

import com.atlassian.labs.plugins3.api.ModuleGenerator;
import com.atlassian.labs.plugins3.api.ModuleName;
import com.atlassian.labs.plugins3.api.PluginDescriptorGenerator;
import com.atlassian.labs.plugins3.api.module.ServletContextParamModuleGenerator;
import com.atlassian.labs.plugins3.api.module.ServletFilterModuleGenerator;
import com.atlassian.labs.plugins3.api.module.ServletModuleGenerator;
import com.atlassian.labs.plugins3.api.module.WebItemModuleGenerator;
import com.atlassian.labs.plugins3.api.module.WebSectionModuleGenerator;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.osgi.factory.OsgiPluginXmlDescriptorParserFactory;
import com.atlassian.plugin.parsers.DescriptorParser;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.XMLWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.atlassian.labs.plugins3.api.module.ModuleBuilderUtil.createElement;

/**
 *
 */
public class DefaultPluginDescriptorGenerator implements PluginDescriptorGenerator<DefaultInfoGenerator>
{
    private final Document doc;

    public DefaultPluginDescriptorGenerator(Document doc)
    {
        this.doc = doc;
    }

    public <M extends PluginDescriptorGenerator> M convertTo(Class<M> generatorClass)
    {
        return instatiate(generatorClass, Document.class, doc);
    }

    private <M,C> M instatiate(Class<M> generatorClass, Class<C> argClass, C argValue)
    {
        try
        {
            return generatorClass.getConstructor(argClass).newInstance(argValue);
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    public DefaultInfoGenerator info()
    {
        return new DefaultInfoGenerator(doc);
    }

    public <M extends ModuleGenerator> M add(String key, Class<M> moduleBuilderClass)
    {
        String tag = moduleBuilderClass.getAnnotation(ModuleName.class).value();
        Element e = doc.getRootElement().addElement(tag);
        e.addAttribute("key", key);
        M builder = instatiate(moduleBuilderClass, Element.class,  e);
        return builder;
    }

    public void add(String key, Element moduleElement)
    {
        moduleElement.addAttribute("key", key);
         doc.getRootElement().add(moduleElement);
    }

    public ServletModuleGenerator addServlet(String key)
    {
        return add(key, ServletModuleGenerator.class);
    }

    public ServletFilterModuleGenerator addServletFilter(String key)
    {
        return add(key, ServletFilterModuleGenerator.class);
    }

    public ServletContextParamModuleGenerator addServletContextParam(String key)
    {
        return add(key, ServletContextParamModuleGenerator.class);
    }

    public WebSectionModuleGenerator addWebSection(String key)
    {
        return add(key, WebSectionModuleGenerator.class);
    }

    public WebItemModuleGenerator addWebItem(String key)
    {
        return add(key, WebItemModuleGenerator.class);
    }

    public void scanForModules(Package rootPackage)
    {
        info().addParameter("scanPackage", rootPackage.getName());
    }
}
