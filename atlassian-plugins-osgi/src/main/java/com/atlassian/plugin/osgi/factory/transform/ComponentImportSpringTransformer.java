package com.atlassian.plugin.osgi.factory.transform;

import org.dom4j.Document;
import org.dom4j.Element;

import java.util.List;
import java.util.ArrayList;
import java.util.jar.Manifest;
import java.io.File;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

/**
 * Transforms component-import module types into Spring configuration
 * @since 2.1
 */
public class ComponentImportSpringTransformer implements SpringTransformer
{
    public void transform(File pluginJar, Manifest mf, List<HostComponentRegistration> regs, Document pluginDoc, Document springDoc)
    {
        Element root = springDoc.getRootElement();
        List<Element> elements = pluginDoc.getRootElement().elements("component-import");
        for (Element component : elements)
        {
            Element osgiReference = root.addElement("osgi:reference");
            osgiReference.addAttribute("id", component.attributeValue("key"));
            String infName = component.attributeValue("interface");
            if (infName != null)
                osgiReference.addAttribute("interface", infName);


            List<Element> compInterfaces = component.elements("interface");
            if (compInterfaces.size() > 0)
            {
                List<String> interfaceNames = new ArrayList<String>();
                for (Element inf : compInterfaces)
                    interfaceNames.add(inf.getTextTrim());

                Element interfaces = osgiReference.addElement("osgi:interfaces");
                for (String name : interfaceNames)
                {
                    Element e = interfaces.addElement("beans:value");
                    e.setText(name);
                }
            }
        }
    }
}
