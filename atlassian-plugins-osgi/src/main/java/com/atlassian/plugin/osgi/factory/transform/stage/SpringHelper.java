package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Helper class for generating a new Spring XML file
 *
 * @since 2.2.0
 */
class SpringHelper
{

    /**
     * Creates a basic spring document with the usual namespaces
     *
     * @return An empty spring XML configuration file with namespaces
     */
    static Document createSpringDocument()
    {
        Document springDoc = DocumentHelper.createDocument();
        Element root = springDoc.addElement("beans");

        root.addNamespace("beans", "http://www.springframework.org/schema/beans");
        root.addNamespace("osgi", "http://www.springframework.org/schema/osgi");
        root.addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.addAttribute(new QName("schemaLocation", new Namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")),
                "http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd\n" +
                        "http://www.springframework.org/schema/osgi http://www.springframework.org/schema/osgi/spring-osgi.xsd");
        root.setName("beans:beans");
        root.addAttribute("default-autowire", "autodetect");
        return springDoc;
    }

    /**
     * Converts an XML document into a byte array
     * @param doc The document
     * @return A byte array of the contents
     */
    static byte[] documentToBytes(Document doc)
    {

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        OutputFormat format = OutputFormat.createPrettyPrint();

        try
        {
            XMLWriter writer = new XMLWriter(bout, format);
            writer.write(doc);
        }
        catch (IOException e)
        {
            throw new PluginTransformationException("Unable to print generated Spring XML", e);
        }

        return bout.toByteArray();
    }
}
