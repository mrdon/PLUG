package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.List;
import java.util.ArrayList;

/**
 * Transforms component tags in the plugin descriptor into the appropriate spring XML configuration file
 *
 * @since 2.2.0
 */
public class ComponentSpringStage implements TransformStage
{
    private static final String SPRING_XML = "META-INF/spring/atlassian-plugins-components.xml";

    public void execute(TransformContext context) throws PluginTransformationException
    {
        if (context.getPluginJar().getEntry(SPRING_XML) == null)
        {
            Document springDoc = SpringHelper.createSpringDocument();
            Element root = springDoc.getRootElement();
            List<Element> elements = context.getDescriptorDocument().getRootElement().elements("component");
            for (Element component : elements)
            {
                Element bean = root.addElement("beans:bean");
                bean.addAttribute("id", component.attributeValue("key"));
                bean.addAttribute("alias", component.attributeValue("alias"));
                bean.addAttribute("class", component.attributeValue("class"));
                if ("true".equalsIgnoreCase(component.attributeValue("public")))
                {
                    Element osgiService = root.addElement("osgi:service");
                    osgiService.addAttribute("id", component.attributeValue("key") + "_osgiService");
                    osgiService.addAttribute("ref", component.attributeValue("key"));

                    List<String> interfaceNames = new ArrayList<String>();
                    List<Element> compInterfaces = component.elements("interface");
                    for (Element inf : compInterfaces)
                    {
                        interfaceNames.add(inf.getTextTrim());
                    }

                    Element interfaces = osgiService.addElement("osgi:interfaces");
                    for (String name : interfaceNames)
                    {
                        Element e = interfaces.addElement("beans:value");
                        e.setText(name);
                    }
                }
            }
            if (root.elements().size() > 0)
            {
                context.getFileOverrides().put(SPRING_XML, SpringHelper.documentToBytes(springDoc));
            }
        }
    }
}
