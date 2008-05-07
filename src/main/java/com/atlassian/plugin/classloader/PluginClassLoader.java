package com.atlassian.plugin.classloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

/**
 * A class loader used to load classes and resources from one plugin.
 */
public class PluginClassLoader extends AbstractClassLoader
{
    private static final Log log = LogFactory.getLog(PluginClassLoader.class);
    private static final String JAR_RESOURCE_SEPARATOR = "!/";

    private final File file;
    private final JarFile jarFile;
    private final List/*<String>*/ innerJars;

    public PluginClassLoader(File file)
    {
        this(file, null);
    }

    public PluginClassLoader(File file, ClassLoader parent)
    {
        super(parent);
        if (file == null || !file.exists())
        {
            throw new IllegalArgumentException("Plugin jar file must not be null and must exist.");
        }
        this.file = file;
        try
        {
            this.jarFile = new JarFile(file);
            innerJars = loadInnerLibraries(this.jarFile);
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("Plugin jar file could not be open for reading.", e);
        }
    }

    private static List loadInnerLibraries(JarFile jarFile) throws IOException
    {
        List innerJars = new ArrayList();
        Enumeration entries = jarFile.entries();
        while (entries.hasMoreElements())
        {
            JarEntry entry = (JarEntry) entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("META-INF/lib/") && name.endsWith(".jar"))
            {
                innerJars.add(name);
            }
        }
        return innerJars;
    }

    public List getInnerJars()
    {
        return innerJars;
    }

    /**
     * Load the named resource from this plugin.  This implementation checks the plugin's contents first
     * then delegates to the system loaders.
     *
     * @param name
     * @return
     */
    public URL getResource(String name)
    {
        final URL url = findResource(name);
        if (url != null) return url;

        return super.getResource(name);
    }

    protected URL findResource(String name)
    {
        try
        {
            // try the outer jar
            if (jarFile.getJarEntry(name) != null)
            {
                return new URL(null, createUrlPath(name), new InnerJarURLStreamHandler());
            }

            // scan each inner jar for the resource
            for (Iterator i = innerJars.iterator(); i.hasNext();)
            {
                String innerJarName = (String) i.next();
                final String urlPath = createUrlPath(innerJarName);

                // scan the inner jar for the desired resource
                try
                {
                    JarEntry innerJarEntry = jarFile.getJarEntry(innerJarName);
                    JarInputStream jin = new JarInputStream(jarFile.getInputStream(innerJarEntry));

                    for (JarEntry entry; (entry = jin.getNextJarEntry()) != null;)
                    {
                        if (entry.getName().equals(name))
                        {
                            final String innerUrlPath = urlPath + JAR_RESOURCE_SEPARATOR + name;
                            return new URL(null, innerUrlPath, new InnerJarURLStreamHandler());
                        }
                    }
                }
                catch (IOException e)
                {
                    log.error("Could not load inner jar", e);
                }
            }
        }
        catch (MalformedURLException e)
        {
            throw new IllegalStateException("Malformed url constructed", e);
        }
        return null; // resource not found
    }

    private String createUrlPath(String innerJarName) throws MalformedURLException
    {
        return file.toURL().toExternalForm() + JAR_RESOURCE_SEPARATOR + innerJarName;
    }

    protected Class findClass(String className) throws ClassNotFoundException
    {
        String resourceName = className.replace('.', '/').concat(".class");
        URL url = findResource(resourceName);
        if (url != null)
        {
            createPackage(className);
            return createClass(url);
        }
        else
        {
            throw new ClassNotFoundException(className);
        }
    }

    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        Class c;
        try
        {
            c = findClass(name);
        }
        catch (ClassNotFoundException ex)
        {
            return super.loadClass(name, resolve);
        }
        return c;
    }

    private Class createClass(URL url)
    {
        byte[] bytes;
        try
            {
                bytes = IOUtils.toByteArray(url.openStream());
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Can't read plugin class resource", e);
        }
        return defineClass(null, bytes, 0, bytes.length);
    }

    private void createPackage(String className)
    {
        // Make sure the package is defined (PLUG-27)
        int pkgOffset = className.lastIndexOf(".");
        if (pkgOffset != -1)
        {
            String pkgName = className.substring(0, pkgOffset);
            Package pkg = getPackage(pkgName);
            if (pkg == null)
            {
                definePackage(pkgName, null, null, null, null, null, null, null);
            }
        }
    }

    public void close()
    {
        try
        {
            this.jarFile.close();
        }
        catch (IOException e)
        {
            log.warn("Could not close the plugin jar ["+file+"]: "+e.getMessage());
            // doesn't matter if we can't close the jar - we are done with it
        }
    }
}
