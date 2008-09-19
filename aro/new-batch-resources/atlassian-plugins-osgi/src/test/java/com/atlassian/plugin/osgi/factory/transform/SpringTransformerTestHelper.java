package com.atlassian.plugin.osgi.factory.transform;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;

import junit.framework.Assert;

public class SpringTransformerTestHelper
{
    public static Element transform(Element pluginRoot, String... xpaths) throws IOException
    {
        ModuleTypeSpringTransformer transformer = new ModuleTypeSpringTransformer();
        Document springDoc = DocumentHelper.createDocument();

        Element springRoot = springDoc.addElement("beans:beans");
        springRoot.addNamespace("beans", "http://www.springframework.org/schema/beans");
        springRoot.addNamespace("osgi", "http://www.springframework.org/schema/osgi");

        transformer.transform(null, pluginRoot.getDocument(), springDoc);

        for (String xp : xpaths)
        {
            XPath xpath = DocumentHelper.createXPath(xp);
            Node node = xpath.selectSingleNode(springRoot);
            if (node == null)
            {
                OutputFormat outformat = OutputFormat.createPrettyPrint();
                XMLWriter writer = new XMLWriter(System.out, outformat);
                writer.write(springDoc);
                writer.flush();
                Assert.fail("Unable to match xpath: "+xp);
            }
        }
        return springRoot;
    }
}
