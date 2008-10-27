package com.atlassian.plugin.osgi.factory.transform;

import org.dom4j.Document;
import org.dom4j.Element;

import java.util.List;
import java.util.ArrayList;
import java.util.jar.Manifest;
import java.io.File;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

/**
 * Transforms components into spring beans
 * @since 2.1
 */
public class ComponentSpringTransformer implements SpringTransformer
{
    public void transform(File pluginJar, Manifest mf, List<HostComponentRegistration> regs, Document pluginDoc, Document springDoc)
    {
        Element root = springDoc.getRootElement();
        List<Element> elements = pluginDoc.getRootElement().elements("component");
        for (Element component : elements)
        {
            Element bean = root.addElement("beans:bean");
            bean.addAttribute("id", component.attributeValue("key"));
            bean.addAttribute("alias", component.attributeValue("alias"));
            bean.addAttribute("class", component.attributeValue("class"));
            if ("true".equalsIgnoreCase(component.attributeValue("public")))
            {
                Element osgiService = root.addElement("osgi:service");
                osgiService.addAttribute("id", component.attributeValue("key")+"_osgiService");
                osgiService.addAttribute("ref", component.attributeValue("key"));

                List<String> interfaceNames = new ArrayList<String>();
                List<Element> compInterfaces = component.elements("interface");
                for (Element inf : compInterfaces)
                    interfaceNames.add(inf.getTextTrim());

                Element interfaces = osgiService.addElement("osgi:interfaces");
                for (String name : interfaceNames)
                {
                    Element e = interfaces.addElement("beans:value");
                    e.setText(name);
                }
            }
        }
    }
}
