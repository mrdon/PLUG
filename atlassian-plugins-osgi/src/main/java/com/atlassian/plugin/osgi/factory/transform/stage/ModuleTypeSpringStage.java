package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import static com.atlassian.plugin.util.validation.ValidationPattern.createPattern;
import static com.atlassian.plugin.util.validation.ValidationPattern.test;
import com.atlassian.plugin.util.validation.ValidationPattern;
import com.atlassian.plugin.util.PluginUtils;
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
    /** The name of the generated Spring XML file for this stage */
    private static final String SPRING_XML = "META-INF/spring/atlassian-plugins-module-types.xml";
    static final String HOST_CONTAINER = "springHostContainer";
    static final String SPRING_HOST_CONTAINER = "com.atlassian.plugin.osgi.bridge.external.SpringHostContainer";

    public static final String BEAN_SOURCE = "Module Type";

    public void execute(TransformContext context) throws PluginTransformationException
    {
        if (SpringHelper.shouldGenerateFile(context, SPRING_XML))
        {
            Document doc = SpringHelper.createSpringDocument();
            Element root = doc.getRootElement();
            List<Element> elements = context.getDescriptorDocument().getRootElement().elements("module-type");
            if (elements.size() > 0)
            {
                context.getExtraImports().add("com.atlassian.plugin.osgi.external");
                context.getExtraImports().add("com.atlassian.plugin.osgi.bridge.external");
                context.getExtraImports().add("com.atlassian.plugin");

                // create a new bean.
                Element hostContainerBean = root.addElement("beans:bean");

                // make sure the new bean id is not already in use.
                context.trackBean(HOST_CONTAINER, BEAN_SOURCE);

                hostContainerBean.addAttribute("id", HOST_CONTAINER);
                hostContainerBean.addAttribute("class", SPRING_HOST_CONTAINER);

                ValidationPattern validation = createPattern().
                        rule(
                            test("@key").withError("The key is required"),
                            test("@class").withError("The class is required"));

                for (Element e : elements)
                {
                    if (!PluginUtils.doesModuleElementApplyToApplication(e, context.getApplicationKeys()))
                    {
                        continue;
                    }
                    validation.evaluate(e);
                    Element bean = root.addElement("beans:bean");

                    String beanId = getBeanId(e);
                    // make sure the new bean id is not already in use.
                    context.trackBean(beanId, BEAN_SOURCE);

                    bean.addAttribute("id", beanId);
                    bean.addAttribute("class", "com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory");

                    Element arg = bean.addElement("beans:constructor-arg");
                    arg.addAttribute("index", "0");
                    arg.addAttribute("ref", HOST_CONTAINER);
                    Element arg2 = bean.addElement("beans:constructor-arg");
                    arg2.addAttribute("index", "1");
                    Element value2 = arg2.addElement("beans:value");
                    value2.setText(e.attributeValue("key"));
                    Element arg3 = bean.addElement("beans:constructor-arg");
                    arg3.addAttribute("index", "2");
                    Element value3 = arg3.addElement("beans:value");
                    value3.setText(e.attributeValue("class"));

                    Element osgiService = root.addElement("osgi:service");
                    String serviceBeanId = getBeanId(e) + "_osgiService";
                    // make sure the new bean id is not already in use.
                    context.trackBean(serviceBeanId, BEAN_SOURCE);

                    osgiService.addAttribute("id", serviceBeanId);
                    osgiService.addAttribute("ref", beanId);
                    osgiService.addAttribute("auto-export", "interfaces");
                }
            }

            if (root.elements().size() > 0)
            {
                context.setShouldRequireSpring(true);
                context.getFileOverrides().put(SPRING_XML, SpringHelper.documentToBytes(doc));
            }
        }
    }

    private String getBeanId(Element e)
    {
        return "moduleType-" + e.attributeValue("key");
    }
}
