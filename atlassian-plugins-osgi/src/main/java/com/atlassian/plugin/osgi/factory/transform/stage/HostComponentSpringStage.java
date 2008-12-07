package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.PropertyBuilder;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.util.Clazz;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import com.atlassian.plugin.PluginParseException;
import org.dom4j.Document;
import org.dom4j.Element;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Constants;

import java.util.*;
import java.util.jar.Manifest;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class HostComponentSpringStage implements TransformStage
{
    private static final Log log = LogFactory.getLog(HostComponentSpringStage.class);
    static final String SPRING_XML = "META-INF/spring/atlassian-plugins-host-components.xml";

    public void execute(TransformContext context) throws PluginTransformationException
    {
        if (context.getPluginJar().getEntry(SPRING_XML) == null)
        {
            Document doc = SpringHelper.createSpringDocument();
            Set<String> hostComponentInterfaceNames = convertRegistrationsToSet(context.getHostComponentRegistrations());
            Set<String> matchedInterfaceNames = new HashSet<String>();
            List<String> innerJarPaths = findJarPaths(context.getManifest());
            try
            {
                findUsedHostComponents(hostComponentInterfaceNames, matchedInterfaceNames, innerJarPaths, new FileInputStream(context.getPluginFile()));
            }
            catch (IOException e)
            {
                throw new PluginParseException("Unable to scan for host components in plugin classes", e);
            }


            List<HostComponentRegistration> matchedRegistrations = new ArrayList<HostComponentRegistration>();
            Element root = doc.getRootElement();
            if (context.getHostComponentRegistrations() != null)
            {
                for (int x = 0; x < context.getHostComponentRegistrations().size(); x++)
                {
                    HostComponentRegistration reg = context.getHostComponentRegistrations().get(x);
                    boolean found = false;
                    for (String name : reg.getMainInterfaces())
                    {
                        if (matchedInterfaceNames.contains(name))
                        {
                            found = true;
                        }
                    }
                    if (!found)
                    {
                        continue;
                    }
                    matchedRegistrations.add(reg);

                    String beanName = reg.getProperties().get(PropertyBuilder.BEAN_NAME);

                    Element osgiService = root.addElement("osgi:reference");
                    osgiService.addAttribute("id", determineId(context.getDescriptorDocument(), beanName, x));

                    if (beanName != null)
                    {
                        osgiService.addAttribute("filter", "(&(bean-name=" + beanName + ")(" + ComponentRegistrar.HOST_COMPONENT_FLAG + "=true))");
                    }

                    Element interfaces = osgiService.addElement("osgi:interfaces");
                    for (String name : reg.getMainInterfaces())
                    {
                        Element e = interfaces.addElement("beans:value");
                        e.setText(name);
                    }
                }
            }
            addImportsForMatchedHostComponents(matchedRegistrations, context.getExtraImports());
            if (root.elements().size() > 0)
            {
                context.getFileOverrides().put(SPRING_XML, SpringHelper.documentToBytes(doc));
            }
        }
    }

    private void addImportsForMatchedHostComponents(List<HostComponentRegistration> matchedRegistrations, List<String> extraImports)
    {
        try
        {
            String list = OsgiHeaderUtil.findReferredPackages(matchedRegistrations);
            extraImports.addAll(Arrays.asList(list.split(",")));
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

        ZipInputStream zin = null;
        try
        {
            zin = new ZipInputStream(jarStream);
            ZipEntry zipEntry;
            while ((zipEntry = zin.getNextEntry()) != null)
            {
                String path = zipEntry.getName();
                if (path.endsWith(".class"))
                {
                    Clazz cls = new Clazz(path, new UnclosableInputStream(zin));
                    Set<String> referredClasses = cls.getReferredClasses();
                    for (String ref : referredClasses)
                    {
                        String name = ref.replaceAll("/", ".").substring(0, ref.length() - ".class".length());
                        if (allHostComponents.contains(name))
                        {
                            matchedHostComponents.add(name);
                        }

                    }
                }
                else
                {
                    if (path.endsWith(".jar") && innerJarPaths.contains(path))
                    {
                        findUsedHostComponents(allHostComponents, matchedHostComponents, null, new UnclosableInputStream(zin));
                    }
                }
            }
        }
        finally
        {
            IOUtils.closeQuietly(zin);
        }
    }

    private List<String> findJarPaths(Manifest
            mf)
    {
        List<String> paths = new ArrayList<String>();
        String cp = mf.getMainAttributes().getValue(Constants.BUNDLE_CLASSPATH);
        if (cp != null)
        {
            for (String entry : cp.split(","))
            {
                entry = entry.trim();
                if (entry.length() == 1)
                {
                    continue;
                }
                else
                {
                    if (entry.endsWith(".jar"))
                    {
                        paths.add(entry);
                    }
                    else
                    {
                        log.warn("Non-jar classpath elements not supported: " + entry);
                    }
                }
            }
        }
        return paths;
    }

    /**
     * Wrapper for the zip input stream to prevent clients from closing it when reading entries
     */
    private static class UnclosableInputStream extends InputStream
    {
        private final InputStream delegate;

        public UnclosableInputStream(InputStream delegate)
        {
            this.delegate = delegate;
        }

        public int read() throws IOException
        {
            return delegate.read();
        }

        @Override
        public void close() throws IOException
        {
            // do nothing
        }
    }

    private String determineId(Document pluginDoc, String beanName, int iteration)
    {
        String id = beanName;
        if (id == null)
        {
            id = "bean" + iteration;
        }

        id = id.replaceAll("#", "LB");

        for (Object element : pluginDoc.getRootElement().elements("component-import"))
        {
            String key = ((Element) element).attributeValue("key");
            if (id.equals(key))
            {
                id += iteration;
                break;
            }
        }
        return id;
    }
}