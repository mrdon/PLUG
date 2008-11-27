package com.atlassian.plugin.osgi.factory.transform.stage;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.File;
import java.io.StringWriter;
import java.util.List;
import java.util.Iterator;

import junit.framework.Assert;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.DefaultPluginManager;
import com.atlassian.plugin.test.PluginJarBuilder;

public class SpringTransformerTestHelper
{

    public static Element transform(TransformStage transformer, Element pluginRoot, String... xpaths) throws IOException, DocumentException
    {
        return transform(transformer, null, pluginRoot, xpaths);
    }

    public static Element transform(TransformStage transformer, List<HostComponentRegistration> regs, Element pluginRoot, String... xpaths) throws IOException, DocumentException
    {
        return transform(transformer, null, regs, pluginRoot, xpaths);
    }

    public static Element transform(TransformStage transformer, File pluginJar, List<HostComponentRegistration> regs, Element pluginRoot, String... xpaths) throws IOException, DocumentException
    {
        if (pluginJar == null)
        {
            StringWriter swriter = new StringWriter();
            OutputFormat outformat = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(swriter, outformat);
            writer.write(pluginRoot.getDocument());
            writer.flush();
            pluginJar = new PluginJarBuilder()
                    .addResource(DefaultPluginManager.PLUGIN_DESCRIPTOR_FILENAME, swriter.toString())
                    .build();
        }
        TransformContext context = new TransformContext(regs, pluginJar, DefaultPluginManager.PLUGIN_DESCRIPTOR_FILENAME);

        transformer.execute(context);

        Iterator<byte[]> itr = context.getFileOverrides().values().iterator();
        if (!itr.hasNext())
        {
            return null;
        }
        Document springDoc = DocumentHelper.parseText(new String(itr.next()));
        Element springRoot = springDoc.getRootElement();

        for (String xp : xpaths)
        {
            XPath xpath = DocumentHelper.createXPath(xp);
            Object obj = xpath.selectObject(springRoot);
            if (obj instanceof Node)
            {
                // test passed
            }
            else if (obj instanceof Boolean)
            {
                if (!((Boolean)obj).booleanValue())
                {
                    printDocument(springDoc);
                    Assert.fail("Unable to match xpath: "+xp);
                }
            }
            else if (obj == null)
            {
                printDocument(springDoc);
                Assert.fail("Unable to match xpath: "+xp);
            }
            else
            {
                printDocument(springDoc);
                Assert.fail("Unexpected result:"+obj);
            }
        }
        return springRoot;
    }

    private static void printDocument(Document springDoc) throws IOException
    {
        OutputFormat outformat = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(System.out, outformat);
        writer.write(springDoc);
        writer.flush();
    }

}
