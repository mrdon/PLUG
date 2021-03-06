package com.atlassian.plugin.osgi.factory.transform.stage;

import aQute.lib.osgi.Clazz;
import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.factory.transform.model.ComponentImport;
import com.atlassian.plugin.osgi.factory.transform.model.SystemExports;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.PropertyBuilder;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.util.ClassBinaryScanner;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.plugin.osgi.util.ClassBinaryScanner.InputStreamResource;
import com.atlassian.plugin.osgi.util.ClassBinaryScanner.ScanResult;
import org.dom4j.Document;
import org.dom4j.Element;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.util.*;
import java.util.jar.Manifest;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class HostComponentSpringStage implements TransformStage
{
    private static final Logger log = LoggerFactory.getLogger(HostComponentSpringStage.class);

    /** Path of generated Spring XML file */
    private static final String SPRING_XML = "META-INF/spring/atlassian-plugins-host-components.xml";

    public static final String BEAN_SOURCE = "Host Component";

    public void execute(TransformContext context) throws PluginTransformationException
    {
        if (SpringHelper.shouldGenerateFile(context, SPRING_XML))
        {
            Document doc = SpringHelper.createSpringDocument();
            Set<String> hostComponentInterfaceNames = convertRegistrationsToSet(context.getHostComponentRegistrations());
            Set<String> matchedInterfaceNames = new HashSet<String>();
            List<String> innerJarPaths = findJarPaths(context.getManifest());
            InputStream pluginStream = null;
            try
            {
                pluginStream = new FileInputStream(context.getPluginFile());
                findUsedHostComponents(hostComponentInterfaceNames, matchedInterfaceNames, innerJarPaths, pluginStream);
            }
            catch (IOException e)
            {
                throw new PluginParseException("Unable to scan for host components in plugin classes", e);
            }
            finally
            {
                IOUtils.closeQuietly(pluginStream);
            }

            List<HostComponentRegistration> matchedRegistrations = new ArrayList<HostComponentRegistration>();
            Element root = doc.getRootElement();
            if (context.getHostComponentRegistrations() != null)
            {
                int index = -1;
                for (HostComponentRegistration reg : context.getHostComponentRegistrations())
                {
                    index++;
                    boolean found = false;
                    for (String name : reg.getMainInterfaces())
                    {
                        if (matchedInterfaceNames.contains(name) || isRequiredHostComponent(context, name))
                        {
                            found = true;
                        }
                    }
                    Set<String> regInterfaces = new HashSet<String>(Arrays.asList(reg.getMainInterfaces()));
                    for (ComponentImport compImport : context.getComponentImports().values())
                    {
                        if (PluginUtils.doesModuleElementApplyToApplication(compImport.getSource(), context.getApplicationKeys()) &&
                            regInterfaces.containsAll(compImport.getInterfaces()))
                        {
                            found = false;
                            break;
                        }
                    }

                    if (!found)
                    {
                        continue;
                    }
                    matchedRegistrations.add(reg);

                    String beanName = reg.getProperties().get(PropertyBuilder.BEAN_NAME);

                    // We don't use Spring DM service references here, because when the plugin is disabled, the proxies
                    // will be marked destroyed, causing undesirable ServiceProxyDestroyedException fireworks.  Since we
                    // know host components won't change over the runtime of the plugin, we can use a simple factory
                    // bean that returns the actual component instance

                    Element osgiService = root.addElement("beans:bean");
                    String beanId = determineId(context.getComponentImports().keySet(), beanName, index);

                    // make sure the new bean id is not already in use.
                    context.trackBean(beanId, BEAN_SOURCE);

                    osgiService.addAttribute("id", beanId);
                    osgiService.addAttribute("lazy-init", "true");

                    // These are strings since we aren't compiling against the osgi-bridge jar
                    osgiService.addAttribute("class", "com.atlassian.plugin.osgi.bridge.external.HostComponentFactoryBean");
                    context.getExtraImports().add("com.atlassian.plugin.osgi.bridge.external");

                    Element e = osgiService.addElement("beans:property");
                    e.addAttribute("name", "filter");

                    e.addAttribute("value", "(&(bean-name=" + beanName + ")(" + ComponentRegistrar.HOST_COMPONENT_FLAG + "=true))");

                    Element listProp = osgiService.addElement("beans:property");
                    listProp.addAttribute("name", "interfaces");
                    Element list = listProp.addElement("beans:list");
                    for (String inf : reg.getMainInterfaces())
                    {
                        Element tmp = list.addElement("beans:value");
                        tmp.setText(inf);
                    }
                }
            }
            addImportsForMatchedHostComponents(matchedRegistrations, context.getSystemExports(), context.getExtraImports());
            if (root.elements().size() > 0)
            {
                context.setShouldRequireSpring(true);
                context.getFileOverrides().put(SPRING_XML, SpringHelper.documentToBytes(doc));
            }
        }
    }

    private void addImportsForMatchedHostComponents(List<HostComponentRegistration> matchedRegistrations,
                                                    SystemExports systemExports, List<String> extraImports)
    {
        try
        {
            Set<String> referredPackages = OsgiHeaderUtil.findReferredPackageNames(matchedRegistrations);

            for (String pkg:referredPackages)
            {
                extraImports.add(systemExports.getFullExport(pkg));
            }
        }
        catch (IOException e)
        {
            throw new PluginTransformationException("Unable to scan for host component referred packages", e);
        }
    }


    private Set<String> convertRegistrationsToSet(List<HostComponentRegistration> regs)
    {
        Set<String> interfaceNames = new HashSet<String>();
        if (regs != null)
        {
            for (HostComponentRegistration reg : regs)
            {
                interfaceNames.addAll(Arrays.asList(reg.getMainInterfaces()));
            }
        }
        return interfaceNames;
    }

    private void findUsedHostComponents(Set<String> allHostComponents, Set<String> matchedHostComponents, List<String> innerJarPaths, InputStream
            jarStream) throws IOException
    {
        Set<String> entries = new HashSet<String>();
        Set<String> superClassNames = new HashSet<String>();
        ZipInputStream zin = null;
        try
        {
            zin = new ZipInputStream(new BufferedInputStream(jarStream));
            ZipEntry zipEntry;
            while ((zipEntry = zin.getNextEntry()) != null)
            {
                String path = zipEntry.getName();
                if (path.endsWith(".class"))
                {
                    entries.add(path.substring(0, path.length() - ".class".length()));
                    final Clazz cls = new Clazz(path, new InputStreamResource(new BufferedInputStream(new UnclosableFilterInputStream(zin))));
                    final ScanResult scanResult = ClassBinaryScanner.scanClassBinary(cls);

                    superClassNames.add(scanResult.getSuperClass());
                    for (String ref : scanResult.getReferredClasses())
                    {
                        String name = TransformStageUtils.jarPathToClassName(ref + ".class");
                        if (allHostComponents.contains(name))
                        {
                            matchedHostComponents.add(name);
                        }
                    }
                }
                else if (path.endsWith(".jar") && innerJarPaths.contains(path))
                {
                    findUsedHostComponents(allHostComponents, matchedHostComponents, Collections.<String>emptyList(), new UnclosableFilterInputStream(zin));
                }
            }
        }
        finally
        {
            IOUtils.closeQuietly(zin);
        }

        addHostComponentsUsedInSuperClasses(allHostComponents, matchedHostComponents, entries, superClassNames);
    }

    /**
     * Searches super classes not in the plugin jar, which have methods that use host components
     * @param allHostComponents The set of all host component classes
     * @param matchedHostComponents The set of host component classes already found
     * @param entries The paths of all files in the jar
     * @param superClassNames All super classes find by classes in the jar
     */
    private void addHostComponentsUsedInSuperClasses(Set<String> allHostComponents, Set<String> matchedHostComponents, Set<String> entries, Set<String> superClassNames)
    {
        for (String superClassName : superClassNames)
        {
            // Only search super classes not in the jar
            if (!entries.contains(superClassName))
            {
                String cls = superClassName.replace('/','.');

                // Ignore java classes including Object
                if (!cls.startsWith("java."))
                {
                    Class spr;
                    try
                    {
                        spr = ClassLoaderUtils.loadClass(cls, this.getClass());
                    }
                    catch (ClassNotFoundException e)
                    {
                        // ignore class not found as it could be from another plugin
                        continue;
                    }

                    // Search methods for parameters that use host components
                    for (Method m : spr.getMethods())
                    {
                        for (Class param : m.getParameterTypes())
                        {
                            if (allHostComponents.contains(param.getName()))
                            {
                                matchedHostComponents.add(param.getName());
                            }
                        }
                    }
                }
            }
        }
    }

    private List<String> findJarPaths(Manifest mf)
    {
        List<String> paths = new ArrayList<String>();
        String cp = mf.getMainAttributes().getValue(Constants.BUNDLE_CLASSPATH);
        if (cp != null)
        {
            for (String entry : cp.split(","))
            {
                entry = entry.trim();
                if (entry.length() != 1 && entry.endsWith(".jar"))
                {
                    paths.add(entry);
                }
                else if (!".".equals(entry))
                {
                    log.warn("Non-jar classpath elements not supported: " + entry);
                }
            }
        }
        return paths;
    }

    /**
     * Wrapper for the zip input stream to prevent clients from closing it when reading entries
     */
    private static class UnclosableFilterInputStream extends FilterInputStream
    {
        public UnclosableFilterInputStream(InputStream delegate)
        {
            super(delegate);
        }

        @Override
        public void close() throws IOException
        {
            // do nothing
        }
    }

    private String determineId(Set<String> hostComponentNames, String beanName, int iteration)
    {
        String id = beanName;
        if (id == null)
        {
            id = "bean" + iteration;
        }

        id = id.replaceAll("#", "LB");

        if (hostComponentNames.contains(id))
        {
            id += iteration;
        }
        return id;
    }

    private boolean isRequiredHostComponent(TransformContext context, String name)
    {
        for (HostComponentRegistration registration : context.getRequiredHostComponents())
        {
            if (Arrays.asList(registration.getMainInterfaces()).contains(name))
            {
                return true;
            }
        }
        return false;
    }
}
