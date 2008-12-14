package com.atlassian.plugin.scripting;

import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.factory.transform.stage.SpringHelper;
import com.atlassian.plugin.scripting.variables.JsScript;

import java.util.Properties;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;

import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import javax.servlet.http.HttpServlet;
import javax.servlet.Filter;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: Dec 13, 2008
 * Time: 2:26:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConventionsToDescriptorStage implements TransformStage
{
    private final Pattern moduleNamePattern = Pattern.compile("([-a-zA-Z0-9_]+)/(.*)\\.js");
    private final Map<String, DefaultConfigBuilder> defaultConfigBuilders = new HashMap<String,DefaultConfigBuilder>()
    {{
        put("servlet", new ServletDefaultConfigBuilder());
        put("servlet-filter", new ServletFilterDefaultConfigBuilder());
    }};
    public void execute(TransformContext context) throws PluginTransformationException
    {
        ScriptingTransformContext ctx = (ScriptingTransformContext) context;
        ScriptManager scriptManager = ctx.getScriptManager();

        DocumentFactory factory = DocumentFactory.getInstance();
        Element root = context.getDescriptorDocument().getRootElement();

        File file = context.getPluginFile();
        ZipInputStream zipInputStream = null;
        try
        {
            try
            {
                zipInputStream = new ZipInputStream(new FileInputStream(file));
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null)
                {
                    String name = entry.getName();
                    if (name.startsWith("shared/"))
                    {
                        scriptManager.runInSharedScope(name, zipInputStream);
                    }
                }
            }
            finally
            {
                IOUtils.closeQuietly(zipInputStream);
            }

            try
            {
                zipInputStream = new ZipInputStream(new FileInputStream(file));
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null)
                {
                    String name = entry.getName();
                    Matcher m = moduleNamePattern.matcher(name);
                    if (m.matches())
                    {
                        String type = m.group(1);
                        JsScript script = scriptManager.run(name, zipInputStream, Collections.<String, Object>emptyMap());
                        if (script.getConfig() != null)
                        {
                            root.add(script.getConfig().getRootElement().createCopy());
                        }
                        else
                        {
                            root.add(defaultConfigBuilders.get(type).getDefault(m.group(2), name));
                        }
                    }
                }
            }
            finally
            {
                IOUtils.closeQuietly(zipInputStream);
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (IOException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        Properties props = loadPluginProperties(context);
        if (props != null)
        {
            String key = props.getProperty("key");
            String version = props.getProperty("version");

            root.addAttribute("key", key);
            root.addAttribute("plugins-version", "3");

            Element info = factory.createElement("plugin-info");
            Element infoVersion = factory.createElement("version", version);
            info.add(infoVersion);
            root.add(info);
        }

        context.getFileOverrides().put("atlassian-plugin.xml", SpringHelper.documentToBytes(context.getDescriptorDocument()));
        context.getExtraImports().add(HttpServlet.class.getPackage().getName());
        context.getExtraImports().add(Filter.class.getPackage().getName());

    }

    private static Element addChild(DocumentFactory factory, Element root, String name, String value)
    {
        Element e = factory.createElement(name);
        e.setText(value);
        root.add(e);
        return e;
    }

    private Properties loadPluginProperties(TransformContext context)
    {
        Properties props = new Properties();
        InputStream in = null;
        try
        {
            in = TransformContext.retrieveStreamFromJar(context.getPluginFile(), "atlassian-plugin.properties");
            if (in == null)
                return null;
            props.load(in);
        }
        catch (IOException e)
        {
            throw new PluginTransformationException("Unable to find or parse atlassian-plugin.properties", e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
        return props;
    }

    private interface DefaultConfigBuilder
    {
        Element getDefault(String name, String path);
    }

    private static class ServletDefaultConfigBuilder implements DefaultConfigBuilder
    {

        public Element getDefault(String name, String path)
        {
            DocumentFactory factory = DocumentFactory.getInstance();
            Element servlet = factory.createElement("servlet");
            servlet.addAttribute("key", "servlet."+name.replace("/", "."));
            servlet.addAttribute("class", path);

            addChild(factory, servlet, "url-pattern", "/"+name);
            addChild(factory, servlet, "url-pattern", "/"+name+"/*");
            return servlet;
        }
    }

    private static class ServletFilterDefaultConfigBuilder implements DefaultConfigBuilder
    {

        public Element getDefault(String name, String path)
        {
            DocumentFactory factory = DocumentFactory.getInstance();
            Element filter = factory.createElement("servlet-filter");
            filter.addAttribute("key", "servlet-filter."+name.replace("/", "."));
            filter.addAttribute("class", path);

            addChild(factory, filter, "url-pattern", "/"+name);
            addChild(factory, filter, "url-pattern", "/"+name+"/*");
            return filter;
        }
    }
}
