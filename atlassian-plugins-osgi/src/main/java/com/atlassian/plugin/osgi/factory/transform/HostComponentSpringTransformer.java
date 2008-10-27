package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.PropertyBuilder;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.util.Clazz;
import com.atlassian.plugin.PluginParseException;

import java.util.*;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.jar.Manifest;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.Element;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Constants;

/**
 * Transforms host components into Spring configuration.  Only transforms host components that it discovered the plugin
 * actually uses by scanning the plugin jar and its inner jars.
 *
 * @since 2.1
 */
public class HostComponentSpringTransformer implements SpringTransformer
{
    private static final Log log = LogFactory.getLog(HostComponentSpringTransformer.class);

    public void transform(File pluginJar, Manifest mf, List<HostComponentRegistration> regs, Document pluginDoc, Document springDoc)
            throws PluginParseException
    {
        Set<String> hostComponentInterfaceNames = convertRegistrationsToSet(regs);
        Set<String> matchedInterfaceNames = new HashSet<String>();
        List<String> innerJarPaths = findJarPaths(mf);
        try
        {
            findUsedHostComponents(hostComponentInterfaceNames, matchedInterfaceNames, innerJarPaths, new FileInputStream(pluginJar));
        }
        catch (IOException e)
        {
            throw new PluginParseException("Unable to scan for host components in plugin classes", e);
        }


        Element root = springDoc.getRootElement();
        if (regs != null)
        {
            for (int x=0; x<regs.size(); x++)
            {
                HostComponentRegistration reg = regs.get(x);
                boolean found = false;
                for (String name : reg.getMainInterfaces())
                {
                    if (matchedInterfaceNames.contains(name)) found = true;
                }
                if (!found) continue;
                
                String beanName = reg.getProperties().get(PropertyBuilder.BEAN_NAME);
                String id = beanName;
                if (id == null)
                    id = "bean"+x;

                id = id.replaceAll("#", "LB");
                Element osgiService = root.addElement("osgi:reference");
                osgiService.addAttribute("id", id);

                if (beanName != null)
                    osgiService.addAttribute("filter", "(&(bean-name="+beanName+")("+ ComponentRegistrar.HOST_COMPONENT_FLAG+"=true))");

                Element interfaces = osgiService.addElement("osgi:interfaces");
                for (String name : reg.getMainInterfaces())
                {
                    Element e = interfaces.addElement("beans:value");
                    e.setText(name);
                }
            }
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

    private void findUsedHostComponents(Set<String> allHostComponents, Set<String> matchedHostComponents, List<String> innerJarPaths, InputStream jarStream) throws IOException
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
                        String name = ref.replaceAll("/",".").substring(0, ref.length() - ".class".length());
                        if (allHostComponents.contains(name))
                        {
                            matchedHostComponents.add(name);
                        }

                    }
                }
                else if (path.endsWith(".jar") && innerJarPaths.contains(path))
                {
                    findUsedHostComponents(allHostComponents, matchedHostComponents, null, new UnclosableInputStream(zin));
                }
            }
        }
        finally
        {
            IOUtils.closeQuietly(zin);
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
                if (entry.length() == 1)
                {
                    continue;
                }
                else if (entry.endsWith(".jar"))
                {
                    paths.add(entry);
                }
                else
                {
                    log.warn("Non-jar classpath elements not supported: "+entry);
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

        public UnclosableInputStream(InputStream delegate) {this.delegate = delegate;}

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
}
