package com.atlassian.plugin.classloader;

import com.atlassian.cache.Cache;
import com.atlassian.cache.memory.MemoryCache;
import com.atlassian.plugin.classloader.url.BytesUrlStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A class loader used to load classes and resources from a given plugin.
 */
public class PluginClassLoader extends URLClassLoader
{
    private static final Log log = LogFactory.getLog(PluginClassLoader.class);

    private static final String PLUGIN_INNER_JAR_PREFIX = "atlassian-plugins-innerjar";

    /**
     * The plugin file.
     */
    private final File pluginFile;

    /**
     * the list of inner jars
     */
    private final List/*<File>*/ pluginInnerJars;

//  Caching
//
//    private final Cache classCache = new MemoryCache(this.getClass().getName() + "#Class");
//
//    private final Cache resourceCache = new MemoryCache(this.getClass().getName() + "#Resource");

    public PluginClassLoader(final File pluginFile)
    {
        this(pluginFile, null);
    }

    public PluginClassLoader(final File pluginFile, ClassLoader parent)
    {
        super(new URL[]{}, parent);
        try
        {
            if (pluginFile == null || !pluginFile.exists())
            {
                throw new IllegalArgumentException("Plugin jar file must not be null and must exist.");
            }
            this.pluginFile = pluginFile;
            addURL(pluginFile.toURL());

            this.pluginInnerJars = new ArrayList();
            initialiseInnerJars();
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Go through all jar inside the plugin JAR
     *
     * @throws IOException
     */
    private void initialiseInnerJars() throws IOException
    {
        final JarFile jarFile = new JarFile(pluginFile);
        try
        {
            for (Enumeration entries = jarFile.entries(); entries.hasMoreElements();)
            {
                final JarEntry jarEntry = (JarEntry) entries.nextElement();
                if (jarEntry.getName().startsWith("META-INF/lib/") && jarEntry.getName().endsWith(".jar"))
                {
                    initialiseInnerJar(jarFile, jarEntry);
                }
            }
        }
        finally
        {
            if (jarFile != null) jarFile.close();
        }
    }

    private void initialiseInnerJar(JarFile jarFile, JarEntry jarEntry) throws IOException
    {
        final File innerJarTmpFile = File.createTempFile(PLUGIN_INNER_JAR_PREFIX, ".jar");
        IOUtils.copy(jarFile.getInputStream(jarEntry), new FileOutputStream(innerJarTmpFile));

        pluginInnerJars.add(innerJarTmpFile);
        addURL(innerJarTmpFile.toURL());
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

    /**
     * Gets the resource from this classloader only
     *
     * @param name the name of the resource
     * @return the URL to the resource, <code>null</code> if the resource was not found
     */
    public URL getLocalResource(String name)
    {
        return findResource(name);
    }

//  Caching
//
//    protected Class findClass(String name) throws ClassNotFoundException
//    {
//        Class c = (Class) classCache.get(name);
//        if (c == null)
//        {
//            c = super.findClass(name);
//            classCache.put(name, c);
//        }
//        return c;
//    }
//
//    public URL findResource(String name)
//    {
//        Resource resource = (Resource) resourceCache.get(name);
//        if (resource == null)
//        {
//            try
//            {
//                final URL url = super.findResource(name);
//                if (url != null)
//                {
//                    resource = new Resource(url);
//                    resourceCache.put(name, resource);
//                }
//            }
//            catch (IOException e)
//            {
//                // ignore
//            }
//        }
//
//        try
//        {
//            return resource != null ? new URL(null, resource.getUrl(), new BytesUrlStreamHandler(resource.getContent())) : null;
//        }
//        catch (MalformedURLException e)
//        {
//            return null;
//        }
//    }

    public void close()
    {
        for (final Iterator innerJars = pluginInnerJars.iterator(); innerJars.hasNext();)
        {
            FileUtils.deleteQuietly((File) innerJars.next());
        }
    }

    List getPluginInnerJars()
    {
        return new ArrayList(pluginInnerJars);
    }

//  Caching
//
//    public InputStream getResourceAsStream(String name)
//    {
//        return super.getResourceAsStream(name);    //To change body of overridden methods use File | Settings | File Templates.
//    }
//
//    private static class Resource
//    {
//        private final String url;
//
//        private final byte[] content;
//
//        public Resource(URL url) throws IOException
//        {
//            this.url = url.toExternalForm();
//            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            IOUtils.copy(url.openStream(), bos);
//            content = bos.toByteArray();
//        }
//
//        public String getUrl()
//        {
//            return url;
//        }
//
//        public byte[] getContent()
//        {
//            return content;
//        }
//    }
}
