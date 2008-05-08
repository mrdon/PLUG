package com.atlassian.plugin.classloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import com.atlassian.cache.Cache;
import com.atlassian.cache.memory.MemoryCache;
import com.atlassian.plugin.url.InnerJarURLStreamHandler;

/**
 * A class loader used to load classes and resources from a given plugin.
 */
public class PluginClassLoader extends AbstractClassLoader
{
    private static final Log log = LogFactory.getLog(PluginClassLoader.class);
    private static final String JAR_RESOURCE_SEPARATOR = "!/";

    /**
     * The {@link URLStreamHandler} to use for resource {@link URL URLs}.
     */
    private static final URLStreamHandler URL_STREAM_HANDLER = new InnerJarURLStreamHandler();

    /**
     * The plugin file.
     */
    private final File file;

    /**
     * The jar file representing the plugin
     */
    private final JarFile jarFile;

    /**
     * The list of inner jars entry names
     */
    private final List/*<String>*/ innerJars;

    /**
     * A cache for classes
     */
    private Cache/*<String,Class>*/ classCache = new MemoryCache(this.getClass().getName() + "#ClassCache");
    /**
     * A resource index
     */
    private Cache /*<String,String>*/ resourceIndex = new MemoryCache(this.getClass().getName() + "#ResourceIndex");

    public PluginClassLoader(final File file)
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
            this.innerJars = listInnerLibraries(this.jarFile);
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("Plugin jar file could not be open for reading." + e.getMessage());
        }
    }

    /**
     * Lists the inner jar contained in the given jar file, inner jars should be located whithin the
     * <code>META-INF/lib</code> folder
     *
     * @param jarFile the jar file to inspect.
     * @return a list of jar entry names
     * @throws IOException if any exception occurs reading the jar file
     */
    private static List/*<String>*/ listInnerLibraries(JarFile jarFile) throws IOException
    {
        final List innerJars = new ArrayList();
        for (Enumeration entries = jarFile.entries(); entries.hasMoreElements();)
        {
            final String entryName = ((JarEntry) entries.nextElement()).getName();
            if (entryName.startsWith("META-INF/lib/") && entryName.endsWith(".jar"))
            {
                innerJars.add(entryName);
            }
        }
        return innerJars;
    }

    /*
     * Enables testing, keep package protected.
     */
    List getInnerJars()
    {
        return new ArrayList(innerJars);
    }

    /**
     * Load the named resource from this plugin. This implementation checks the plugin's contents first
     * then delegates to the system loaders.
     *
     * @param name the name of the resource.
     * @return the URL to the resource, <code>null</code> if the resource was not found.
     */
    public URL getResource(String name)
    {
        final URL url = findResource(name);
        return url != null ? url : super.getResource(name);
    }

    protected URL findResource(String name)
    {
        try
        {
            // check if the resource has been indexed
            String urlPath = (String) resourceIndex.get(name);

            // try to find the resource in the plugin jar itself
            if (urlPath == null && jarFile.getJarEntry(name) != null)
            {
                urlPath = createUrlPath(name);
            }

            // try to find the resource in the innner jars
            if (urlPath == null)
            {
                for (Iterator innerJarNames = innerJars.iterator(); innerJarNames.hasNext();)
                {
                    final String innerJarName = (String) innerJarNames.next();
                    final String innerJarUrlPath = createUrlPath(innerJarName);

                    // scan the inner jar for the resource
                    try
                    {
                        final JarInputStream jin = new JarInputStream(jarFile.getInputStream(jarFile.getJarEntry(innerJarName)));
                        JarEntry entry;
                        while (urlPath == null && (entry = jin.getNextJarEntry()) != null)
                        {
                            if (entry.getName().equals(name))
                            {
                                urlPath = innerJarUrlPath + JAR_RESOURCE_SEPARATOR + name;
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        log.error("Could not load inner jar", e);
                    }

                }
            }

            if (urlPath != null)
            {
                // add it to the cache
                resourceIndex.put(name, urlPath);
                return getResourceUrl(urlPath);
            }

            return null; // resource not found
        }
        catch (MalformedURLException e)
        {
            throw new IllegalStateException("Malformed url constructed: " + e.getMessage());
        }
    }

    /**
     * Get the URL to the resource
     *
     * @param urlPath the String representation of the URL
     * @return the URL
     * @throws MalformedURLException if the urlPath cannot be translated into a valid URL
     */
    private static URL getResourceUrl(String urlPath) throws MalformedURLException
    {
        return new URL(null, urlPath, URL_STREAM_HANDLER);
    }

    private String createUrlPath(String innerJarName) throws MalformedURLException
    {
        return file.toURL().toExternalForm() + JAR_RESOURCE_SEPARATOR + innerJarName;
    }

    protected Class findClass(String className) throws ClassNotFoundException
    {
        final String resourceName = className.replace('.', '/').concat(".class");
        final URL url = findResource(resourceName);
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
        Class c = (Class) classCache.get(name);
        if (c != null) return c;

        try
        {
            c = findClass(name);
        }
        catch (ClassNotFoundException ex)
        {
            return super.loadClass(name, resolve);
        }
        classCache.put(name, c);
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
            throw new IllegalStateException("Can't read plugin class resource: " + e.getMessage());
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
            log.warn("Could not close the plugin jar [" + file + "]: " + e.getMessage());
            // doesn't matter if we can't close the jar - we are done with it
        }
    }
}
