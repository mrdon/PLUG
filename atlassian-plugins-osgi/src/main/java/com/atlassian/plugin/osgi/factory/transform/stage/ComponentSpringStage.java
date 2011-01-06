package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.multitenant.MultiTenantContext;
import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import static com.atlassian.plugin.util.validation.ValidationPattern.createPattern;
import static com.atlassian.plugin.util.validation.ValidationPattern.test;

import com.atlassian.plugin.util.validation.ValidationPattern;
import com.atlassian.plugin.util.PluginUtils;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarInputStream;

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
                bean.addAttribute("autowire", "default");

                // alias attribute in atlassian-plugin gets converted into alias element.
                if (!StringUtils.isBlank(component.attributeValue("alias")))
                {
                    Element alias = root.addElement("beans:alias");
                    alias.addAttribute("name", beanId);
                    alias.addAttribute("alias", component.attributeValue("alias"));
                }

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

                // If stateful is true, then we declare the class to be a MultiTenantComponentFactoryBean, which instantiates
                // a proxy that a different instance for each tenant sits behind.
                boolean stateful = Boolean.parseBoolean(component.attributeValue("stateful"));
                if (stateful && MultiTenantContext.isEnabled())
                {
                    bean.addAttribute("class", "com.atlassian.plugin.osgi.bridge.external.MultiTenantComponentFactoryBean");
                    bean.addElement("beans:property").addAttribute("name", "implementation")
                            .addAttribute("value", component.attributeValue("class"));
                    // Lazy load means it will be loaded the first time something calls it, rather than when the tenant
                    // is added
                    Element lazy = bean.addElement("beans:property").addAttribute("name", "lazyLoad");
                    if (component.attribute("lazy") == null || Boolean.parseBoolean(component.attributeValue("lazy")))
                    {
                        lazy.addAttribute("value", "true");
                    }
                    else
                    {
                        lazy.addAttribute("value", "false");
                    }
                    if (!interfaceNames.isEmpty())
                    {
                        Element interfaces = bean.addElement("beans:property").addAttribute("name", "interfaces")
                                .addElement("beans:list");
                        for (String interfaceName : interfaceNames)
                        {
                            Element e = interfaces.addElement("beans:value");
                            e.setText(interfaceName);
                        }
                    }
                    ensureMultiTenantImported(context);
                }
                else
                {
                    bean.addAttribute("class", component.attributeValue("class"));
                }

                if ("true".equalsIgnoreCase(component.attributeValue("public")))
                {
                    Element osgiService = root.addElement("osgi:service");
                    osgiService.addAttribute("id", component.attributeValue("key") + "_osgiService");
                    osgiService.addAttribute("ref", component.attributeValue("key"));


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

            // calculate the required interfaces to be imported. this is (all the classes) - (classes available in the plugin).
            Set<String> requiredInterfaces;
            try
            {
                requiredInterfaces = calculateRequiredImports(context.getPluginFile(),
                                                              declaredInterfaces,
                                                              context.getBundleClassPathJars());
            }
            catch (PluginTransformationException e)
            {
                throw new PluginTransformationException("Error while calculating import manifest", e);
            }

            // dump all the outstanding imports as extra imports.
            context.getExtraImports().addAll(TransformStageUtils.getPackageNames(requiredInterfaces));
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

    void ensureMultiTenantImported(TransformContext context)
    {
        if (!context.getExtraImports().contains("com.atlassian.plugin.osgi.bridge.external"))
        {
            context.getExtraImports().add("com.atlassian.plugin.osgi.bridge.external");
        }
    }

    /**
     * Calculate the the interfaces that need to be imported.
     *
     * @return the set of interfaces that cannot be resolved in the pluginFile.
     */
    private Set<String> calculateRequiredImports(final File pluginFile,
                                                 final Set<String> declaredInterfaces,
                                                 final Set<String> innerJars)
    {
        // we only do it if at least one interface is declared as part of component element.
        if (declaredInterfaces.size() > 0)
        {
            // scan for class files of interest in the jar file, not including classes in inner jars.
            final Set<String> shallowMatches;
            FileInputStream fis = null;
            JarInputStream jarStream = null;
            try
            {
                fis = new FileInputStream(pluginFile);
                jarStream = new JarInputStream(fis);
                shallowMatches =TransformStageUtils.scanJarForItems(jarStream,
                                                                    declaredInterfaces,
                                                                    TransformStageUtils.JarEntryToClassName.INSTANCE);
            }
            catch (final IOException ioe)
            {
                throw new PluginTransformationException("Error reading jar:" + pluginFile.getName(), ioe);
            }
            finally
            {
                TransformStageUtils.closeNestedStreamQuietly(jarStream, fis);
            }

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
