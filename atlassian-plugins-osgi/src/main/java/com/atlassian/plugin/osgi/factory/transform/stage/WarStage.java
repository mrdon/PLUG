package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.PluginParseException;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.jar.JarEntry;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: Dec 11, 2008
 * Time: 9:24:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class WarStage implements TransformStage
{
    public void execute(TransformContext context) throws PluginTransformationException
    {

        Document webDoc = getWebDoc(context);
        DocumentFactory factory = DocumentFactory.getInstance();
        Document pluginDoc = factory.createDocument();

        String version = getVersionFromFilename(context.getPluginFile());
        String id = webDoc.getRootElement().attributeValue("id");

        Element root = factory.createElement("atlassian-plugin");
        pluginDoc.add(root);
        root.addAttribute("key", id);
        root.addAttribute("plugins-version", "2");
        Element pluginInfo = factory.createElement("plugin-info");
        Element pluginVersion = factory.createElement("version");
        pluginVersion.setText(version);
        pluginInfo.add(pluginVersion);
        root.add(pluginInfo);

        Map<String,Element> apServlets = new HashMap<String,Element>();
        for (Iterator i = webDoc.getRootElement().elementIterator("servlet"); i.hasNext(); )
        {
            Element servlet = (Element) i.next();
            String name = servlet.element("servlet-name").getText();
            String cls = servlet.element("servlet-class").getText();

            Element apServlet = factory.createElement("servlet");
            apServlet.addAttribute("key", name);
            apServlet.addAttribute("class", cls);
            apServlets.put(name, apServlet);
        }

        for (Iterator i = webDoc.getRootElement().elementIterator("servlet-mapping"); i.hasNext(); )
        {
            Element servlet = (Element) i.next();
            String name = servlet.element("servlet-name").getText();
            String pattern = servlet.element("url-pattern").getText();

            Element apServlet = apServlets.get(name);
            Element urlPattern = factory.createElement("url-pattern");
            urlPattern.setText(pattern);
            apServlet.add(urlPattern);
        }

        for (Element child : apServlets.values())
        {
            root.add(child);
        }

        context.getFileOverrides().put("atlassian-plugin.xml", SpringHelper.documentToBytes(pluginDoc));

        StringBuilder cp = new StringBuilder();
        cp.append("WEB-INF/classes");
        for (Enumeration<JarEntry> e = context.getPluginJar().entries(); e.hasMoreElements(); )
        {
            JarEntry entry = e.nextElement();
            String name = entry.getName();
            if (name.startsWith("WEB-INF/lib") && name.endsWith(".jar"))
            {
                cp.append(",").append(name);
            }
        }
        context.getBndInstructions().put("Bundle-ClassPath", cp.toString());

    }

    private String getVersionFromFilename(File pluginFile)
    {
        int dashPos = pluginFile.getName().lastIndexOf("-");
        int dotPos = pluginFile.getName().lastIndexOf(".");
        return pluginFile.getName().substring(dashPos+1, dotPos);
    }

    private Document getWebDoc(TransformContext context)
    {
        InputStream webStream = null;
        Document webDoc = null;
        try
        {
            webStream = context.retrieveStreamFromJar(context.getPluginFile(), "WEB-INF/web.xml");
            SAXReader reader = new SAXReader();
            webDoc = reader.read(webStream);
        }
        catch (final DocumentException e)
        {
            throw new PluginParseException("Cannot parse XML plugin descriptor", e);
        }
        catch (IOException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        finally
        {
            IOUtils.closeQuietly(webStream);
        }
        return webDoc;
    }
}
