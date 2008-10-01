package com.atlassian.plugin.osgi.factory.transform;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

public class SpringTransformerTestHelper
{
    public static Element transform(SpringTransformer transformer, Element pluginRoot, String... xpaths) throws IOException
    {
        return transform(transformer, null, pluginRoot, xpaths);
    }

    public static Element transform(SpringTransformer transformer, List<HostComponentRegistration> regs, Element pluginRoot, String... xpaths) throws IOException
    {
        Document springDoc = DocumentHelper.createDocument();

        Element springRoot = springDoc.addElement("beans:beans");
        springRoot.addNamespace("beans", "http://www.springframework.org/schema/beans");
        springRoot.addNamespace("osgi", "http://www.springframework.org/schema/osgi");

        transformer.transform(regs, pluginRoot.getDocument(), springDoc);

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
