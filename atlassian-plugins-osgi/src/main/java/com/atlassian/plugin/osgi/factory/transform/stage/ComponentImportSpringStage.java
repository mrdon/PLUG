package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.List;
import java.util.ArrayList;

/**
 * Transforms component imports into a Spring XML file
 *
 * @since 2.2.0
 */
public class ComponentImportSpringStage implements TransformStage
{
    /** Path of generated Spring XML file */
    private static final String SPRING_XML = "META-INF/spring/atlassian-plugins-component-imports.xml";

    public void execute(TransformContext context) throws PluginTransformationException
    {
        if (SpringHelper.shouldGenerateFile(context, SPRING_XML))
        {
            Document springDoc = SpringHelper.createSpringDocument();
            Element root = springDoc.getRootElement();
            List<Element> elements = context.getDescriptorDocument().getRootElement().elements("component-import");
            for (Element component : elements)
            {
                Element osgiReference = root.addElement("osgi:reference");
                osgiReference.addAttribute("id", component.attributeValue("key"));
                String infName = component.attributeValue("interface");
                if (infName != null)
                {
                    osgiReference.addAttribute("interface", infName);
                    context.getExtraImports().add(infName.substring(0, infName.lastIndexOf('.')));
                }


                List<Element> compInterfaces = component.elements("interface");
                if (compInterfaces.size() > 0)
                {
                    List<String> interfaceNames = new ArrayList<String>();
                    for (Element inf : compInterfaces)
                    {
                        interfaceNames.add(inf.getTextTrim());
                    }

                    Element interfaces = osgiReference.addElement("osgi:interfaces");
                    for (String name : interfaceNames)
                    {
                        Element e = interfaces.addElement("beans:value");
                        e.setText(name);
                        context.getExtraImports().add(name.substring(0, name.lastIndexOf('.')));
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