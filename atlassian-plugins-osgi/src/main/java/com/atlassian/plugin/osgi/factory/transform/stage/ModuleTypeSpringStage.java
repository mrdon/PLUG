package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.List;

/**
 * Transforms module-type elements into the appropriate Spring XML configuration file
 *
 * @since 2.2.0
 */
public class ModuleTypeSpringStage implements TransformStage
{
    private static final String SPRING_XML = "META-INF/spring/atlassian-plugins-module-types.xml";

    public void execute(TransformContext context) throws PluginTransformationException
    {
        if (context.getPluginJar().getEntry(SPRING_XML) == null)
        {
            Document doc = SpringHelper.createSpringDocument();
            Element root = doc.getRootElement();
            List<Element> elements = context.getDescriptorDocument().getRootElement().elements("module-type");
            if (elements.size() > 0)
            {
                context.getExtraImports().add("com.atlassian.plugin.osgi.external");
                context.getExtraImports().add("com.atlassian.plugin");
            }
            for (Element e : elements)
            {
                Element bean = root.addElement("beans:bean");
                bean.addAttribute("id", getBeanId(e));
                bean.addAttribute("class", "com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory");

                Element arg = bean.addElement("beans:constructor-arg");
                arg.addAttribute("index", "0");
                Element value = arg.addElement("beans:value");
                value.setText(e.attributeValue("key"));
                Element arg2 = bean.addElement("beans:constructor-arg");
                arg2.addAttribute("index", "1");
                Element value2 = arg2.addElement("beans:value");
                value2.setText(e.attributeValue("class"));

                Element osgiService = root.addElement("osgi:service");
                osgiService.addAttribute("id", getBeanId(e) + "_osgiService");
                osgiService.addAttribute("ref", getBeanId(e));
                osgiService.addAttribute("auto-export", "interfaces");
            }

            if (root.elements().size() > 0)
            {
                context.getFileOverrides().put(SPRING_XML, SpringHelper.documentToBytes(doc));
            }
        }
    }

    private String getBeanId(Element e)
    {
        return "moduleType-" + e.attributeValue("key");
    }
}
