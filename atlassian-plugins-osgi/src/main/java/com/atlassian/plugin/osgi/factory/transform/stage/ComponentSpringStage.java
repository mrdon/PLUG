package com.atlassian.plugin.osgi.factory.transform.stage;

import static com.atlassian.plugin.osgi.factory.transform.stage.TransformStageUtils.scanJarForItems;
import static com.atlassian.plugin.util.validation.ValidationPattern.createPattern;
import static com.atlassian.plugin.util.validation.ValidationPattern.test;

import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.plugin.util.validation.ValidationPattern;

import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public void execute(final TransformContext context) throws PluginTransformationException
    {
        if (SpringHelper.shouldGenerateFile(context, SPRING_XML))
        {
            final Document springDoc = SpringHelper.createSpringDocument();
            final Element root = springDoc.getRootElement();
            @SuppressWarnings("unchecked")
            final List<Element> elements = context.getDescriptorDocument().getRootElement().elements("component");

            final ValidationPattern validation = createPattern().
                rule(
                    test("@key").withError("The key is required"),
                    test("@class").withError("The class is required"),
                    test("not(@public) or interface or @interface").
                        withError("Interfaces must be declared for public components"),
                    test("not(service-properties) or count(service-properties/entry[@key and @value]) > 0").
                        withError("The service-properties element must contain at least one entry element with key and value attributes")
            );

            final Set<String> declaredInterfaces = new HashSet<String>();

            for (final Element component : elements)
            {
                if (!PluginUtils.doesModuleElementApplyToApplication(component, context.getApplicationKeys()))
                {
                    continue;
                }
                validation.evaluate(component);

                final String beanId = component.attributeValue("key");
                // make sure the new bean id is not already in use.
                context.trackBean(beanId, BEAN_SOURCE);

                final Element bean = root.addElement("beans:bean");
                bean.addAttribute("id", beanId);
                bean.addAttribute("alias", component.attributeValue("alias"));
                bean.addAttribute("class", component.attributeValue("class"));
                bean.addAttribute("autowire", "default");
                if ("true".equalsIgnoreCase(component.attributeValue("public")))
                {
                    final Element osgiService = root.addElement("osgi:service");
                    osgiService.addAttribute("id", component.attributeValue("key") + "_osgiService");
                    osgiService.addAttribute("ref", component.attributeValue("key"));

                    final List<String> interfaceNames = new ArrayList<String>();
                    @SuppressWarnings("unchecked")
                    final List<Element> compInterfaces = component.elements("interface");
                    for (final Element inf : compInterfaces)
                    {
                        interfaceNames.add(inf.getTextTrim());
                    }
                    if (component.attributeValue("interface") != null)
                    {
                        interfaceNames.add(component.attributeValue("interface"));
                    }

                    // Collect for the interface names which will be used for import generation.
                    declaredInterfaces.addAll(interfaceNames);

                    final Element interfaces = osgiService.addElement("osgi:interfaces");
                    for (final String name : interfaceNames)
                    {
                        ensureExported(name, context);
                        final Element e = interfaces.addElement("beans:value");
                        e.setText(name);
                    }

                    final Element svcprops = component.element("service-properties");
                    if (svcprops != null)
                    {
                        final Element targetSvcprops = osgiService.addElement("osgi:service-properties");
                        @SuppressWarnings("unchecked")
                        final List<Element> entries = svcprops.elements("entry");
                        for (final Element prop : entries)
                        {
                            final Element e = targetSvcprops.addElement("beans:entry");
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

            // calculate the required interfaces to be imported. this is (all the classes) - (classes available in the plugin).
            Set<String> requiredInterfaces = calculateRequiredImports(context.getPluginFile(), declaredInterfaces, context.getBundleClassPathJars());

            // dump all the outstanding imports as extra imports.
            context.getExtraImports().addAll(TransformStageUtils.getPackageNames(requiredInterfaces));
        }
    }

    private void ensureExported(final String className, final TransformContext context)
    {
        final String pkg = className.substring(0, className.lastIndexOf('.'));
        if (!context.getExtraExports().contains(pkg))
        {
            final String fileName = className.replace('.', '/') + ".class";

            if (context.getPluginArtifact().doesResourceExist(fileName))
            {
                context.getExtraExports().add(pkg);
            }
        }
    }

    /**
     * Calculate the the interfaces that need to be imported.
     *
     * @return the set of interfaces that cannot be resolved in the pluginFile.
     */
    private Set<String> calculateRequiredImports(final File pluginFile, final Set<String> declaredInterfaces, final Set<String> innerJars) 
    {
        // we only do it if at least one interface is declared as part of component element.
        if (declaredInterfaces.size() > 0)
        {
            // scan for class files of interest in the jar file, not including classes in inner jars.
            final Set<String> shallowMatches = scanJarForItems(new TransformStageUtils.JarFileStream(pluginFile), declaredInterfaces,
                TransformStageUtils.JarEntryToClassName.INSTANCE);

            // the outstanding set = declared set - shallow match set
            final Set<String> remainders = Sets.newHashSet(Sets.difference(declaredInterfaces, shallowMatches));

            // if all the interfaces are not yet satisfied, we have to scan inner jars as well.
            // this is, of course, subject to the availability of qualified inner jars.
            if ((remainders.size() > 0) && (innerJars.size() > 0))
            {
                remainders.removeAll(TransformStageUtils.scanInnerJars(pluginFile, innerJars, remainders));
            }

            return Collections.unmodifiableSet(remainders);
        }

        // if no need to import.
        return Collections.emptySet();
    }
}