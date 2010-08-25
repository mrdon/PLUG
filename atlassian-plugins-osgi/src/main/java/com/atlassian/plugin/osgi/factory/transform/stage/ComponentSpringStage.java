package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import static com.atlassian.plugin.util.validation.ValidationPattern.createPattern;
import static com.atlassian.plugin.util.validation.ValidationPattern.test;

import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.plugin.util.ClassUtils;
import com.atlassian.plugin.util.validation.ValidationPattern;
import com.atlassian.plugin.util.PluginUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import com.google.common.collect.Sets;
import com.google.common.base.Predicate;

/**
 * Transforms component tags in the plugin descriptor into the appropriate spring XML configuration file
 *
 * @since 2.2.0
 */
public class ComponentSpringStage implements TransformStage
{
    /** Path of generated Spring XML file */
    private static final String SPRING_XML = "META-INF/spring/atlassian-plugins-components.xml";

    public static final String BEAN_SOURCE = "Plugin Component";

    private static final Logger LOG = LoggerFactory.getLogger(ComponentSpringStage.class);

    public void execute(TransformContext context) throws PluginTransformationException
    {
        if (SpringHelper.shouldGenerateFile(context, SPRING_XML))
        {
            Document springDoc = SpringHelper.createSpringDocument();
            Element root = springDoc.getRootElement();
            List<Element> elements = context.getDescriptorDocument().getRootElement().elements("component");

            ValidationPattern validation = createPattern().
                    rule(
                        test("@key").withError("The key is required"),
                        test("@class").withError("The class is required"),
                        test("not(@public) or interface or @interface").withError("Interfaces must be declared for public components"),
                        test("not(service-properties) or count(service-properties/entry[@key and @value]) > 0")
                                .withError("The service-properties element must contain at least one entry element with key and value attributes"));

            final Set<String> declaredInterfaces = new HashSet<String>();

            for (Element component : elements)
            {
                if (!PluginUtils.doesModuleElementApplyToApplication(component, context.getApplicationKeys()))
                {
                    continue;
                }
                validation.evaluate(component);

                String beanId = component.attributeValue("key");
                // make sure the new bean id is not already in use.
                context.trackBean(beanId, BEAN_SOURCE);

                Element bean = root.addElement("beans:bean");
                bean.addAttribute("id", beanId);
                bean.addAttribute("alias", component.attributeValue("alias"));
                bean.addAttribute("class", component.attributeValue("class"));
                bean.addAttribute("autowire", "default");
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
                    if (component.attributeValue("interface") != null)
                    {
                        interfaceNames.add(component.attributeValue("interface"));
                    }

                    // Collect for the interface names which will be used for import generation.
                    declaredInterfaces.addAll(interfaceNames);

                    Element interfaces = osgiService.addElement("osgi:interfaces");
                    for (String name : interfaceNames)
                    {
                        ensureExported(name, context);
                        Element e = interfaces.addElement("beans:value");
                        e.setText(name);
                    }

                    Element svcprops = component.element("service-properties");
                    if (svcprops != null)
                    {
                        Element targetSvcprops = osgiService.addElement("osgi:service-properties");
                        for (Element prop : new ArrayList<Element>(svcprops.elements("entry")))
                        {
                            Element e = targetSvcprops.addElement("beans:entry");
                            e.addAttribute("key", prop.attributeValue("key"));
                            e.addAttribute("value", prop.attributeValue("value"));
                        }
                    }
                }
            }

            if (root.elements().size() > 0)
            {
                context.setShouldRequireSpring(true);
                context.getFileOverrides().put(SPRING_XML, SpringHelper.documentToBytes(springDoc));
            }

            // calculate the required imports this is (all the classes) - (classes available in the plugin)
            context.getExtraImports().addAll(calculateRequiredImports(context.getPluginFile(), declaredInterfaces));
        }
    }

    private void ensureExported(String className, TransformContext context)
    {
        String pkg = className.substring(0, className.lastIndexOf('.'));
        if (!context.getExtraExports().contains(pkg))
        {
            String fileName = className.replace('.','/') + ".class";
            
            if (context.getPluginArtifact().doesResourceExist(fileName))
            {
                context.getExtraExports().add(pkg);
            }
        }
    }

    private Set<String> calculateRequiredImports(File pluginFile, Set<String> declaredInterfaces)
    {
        // we only do it if interfaces are declared as part of component element.
        if (declaredInterfaces.size() > 0)
        {
            // calculate all the available classes in the plugin.
            final Set<String> pluginClasses;
            try
            {
                pluginClasses =  TransformStageUtils.extractPluginClasses(new FileInputStream(pluginFile));
            }
            catch (IOException e)
            {
                throw new PluginTransformationException("error while reading from plugin jar", e);
            }

            final Set<String> requiredPackages = new HashSet<String>();

            // we will only import only interfaces not available from the plugin bundle.
            for(String inf:declaredInterfaces)
            {
                // if the plugin doesn't contain the interface.
                if (!pluginClasses.contains(inf))
                {
                    // we then need to import the package
                    requiredPackages.add(TransformStageUtils.getPackageName(inf));
                }
            }

            return requiredPackages;
        }

        // if no need to import.
        return Collections.emptySet();
    }
}