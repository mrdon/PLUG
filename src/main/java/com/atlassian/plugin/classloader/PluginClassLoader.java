package com.atlassian.plugin.classloader;

import com.atlassian.core.util.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A class loader used to load classes and resources from a given plugin.
 */
public class PluginClassLoader extends ClassLoader
{
    private static final String PLUGIN_INNER_JAR_PREFIX = "atlassian-plugins-innerjar";

    /**
     * The plugin file.
     */
    private final File pluginFile;

    /**
     * the list of inner jars
     */
    private final List/*<File>*/ pluginInnerJars;
    /**
     * Mapping of <String> names to <URL>s.
     */
    private final Map entryMappings = new HashMap();

    public PluginClassLoader(final File pluginFile)
    {
        this(pluginFile, null);
    }

    public PluginClassLoader(final File pluginFile, ClassLoader parent)
    {
        super(parent);
        try
        {
            if (pluginFile == null || !pluginFile.exists())
            {
                throw new IllegalArgumentException("Plugin jar file must not be null and must exist.");
            }
            this.pluginFile = pluginFile;
            this.pluginInnerJars = new ArrayList();
            initializeFromJar(pluginFile, true);
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
     * @param pluginFile the file to scan
     */
    private void initializeFromJar(File pluginFile, boolean isOuterJar) throws IOException
    {
        final JarFile jarFile = new JarFile(pluginFile);
        try
        {
            for (Enumeration entries = jarFile.entries(); entries.hasMoreElements();)
            {
                final JarEntry jarEntry = (JarEntry) entries.nextElement();
                if(isOuterJar && jarEntry.getName().startsWith("META-INF/lib/") && jarEntry.getName().endsWith(".jar"))
                    initialiseInnerJar(jarFile, jarEntry);
                else
                    addEntryMapping(jarEntry, pluginFile, isOuterJar);
            }
        }
        finally
        {
            jarFile.close();
        }
    }

    private void initialiseInnerJar(JarFile jarFile, JarEntry jarEntry) throws IOException
    {
        final File innerJarTmpFile = File.createTempFile(PLUGIN_INNER_JAR_PREFIX, ".jar");
        InputStream inputStream = jarFile.getInputStream(jarEntry);
        FileOutputStream fileOutputStream = new FileOutputStream(innerJarTmpFile);
        FileUtils.copy(inputStream, fileOutputStream);
        initializeFromJar(innerJarTmpFile, false);
        pluginInnerJars.add(innerJarTmpFile);
        inputStream.close();
        fileOutputStream.close();
    }

    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        // First check if it's already been loaded
        Class c = findLoadedClass(name);
        if (c != null)
            return c;

        // If not, look inside the plugin before searching the parent.
        try
        {
            String path = name.replace('.', '/').concat(".class");
            if (isEntryInPlugin(path))
            {
                try
                {
                    Class aClass = loadClassFromPlugin(name, path);
                    return aClass;
                }
                catch (IOException e)
                {
                    throw new ClassNotFoundException(name, e);
                }
            }
            else
            {
                return findClass(name);
            }
        }
        catch (ClassNotFoundException ex)
        {
            return super.loadClass(name, resolve);
        }
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
        if (isEntryInPlugin(name))
        {
            return (URL) entryMappings.get(name);
        }
        else
        {
            return super.getResource(name);
        }
    }

    public void close()
    {
        for (final Iterator innerJars = pluginInnerJars.iterator(); innerJars.hasNext();)
        {
            try
            {
                ((File) innerJars.next()).delete();
            } catch (Exception ignored) {}
        }
    }

    List getPluginInnerJars()
    {
        return new ArrayList(pluginInnerJars);
    }

    private void initializePackage(String name) throws IOException
    {
        int i = name.lastIndexOf('.');
        if (i != -1)
        {
            String pkgname = name.substring(0, i);
            // Check if package already loaded.
            Package pkg = getPackage(pkgname);
            if (pkg == null)
            {
                definePackage(pkgname, null, null, null, null, null, null, null);
            }
        }
    }

    private Class loadClassFromPlugin(String className, String path) throws IOException
    {
        URL resourceURL = (URL) entryMappings.get(path);
        InputStream inputStream = resourceURL.openStream();
        byte[] bytez = toByteArray(inputStream);
        FileUtils.shutdownStream(inputStream);
        initializePackage(className);
        return defineClass(className, bytez, 0, bytez.length);
    }
    
    
    private byte[] toByteArray(InputStream input) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FileUtils.copy(input, output);
        return output.toByteArray();
    }

    private URL getUrlOfResourceInJar(String name, File jarFile)
    {
        try
        {
            URL url = new URL(new URL("jar:file:" + jarFile.getAbsolutePath() + "!/"), name, new NonLockingJarHandler());
            return url;
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private boolean isEntryInPlugin(String name)
    {
        return entryMappings.containsKey(name);
    }

    private void addEntryMapping(JarEntry jarEntry, File jarFile, boolean overrideExistingEntries)
    {
        if(overrideExistingEntries)
        {
            addEntryUrl(jarEntry, jarFile);
        }
        else
        {
            if(!entryMappings.containsKey(jarEntry.getName()))
            {
                addEntryUrl(jarEntry, jarFile);
            }
        }
    }

    private void addEntryUrl(JarEntry jarEntry, File jarFile)
    {
        entryMappings.put(jarEntry.getName(), getUrlOfResourceInJar(jarEntry.getName(), jarFile));
    }
}