package com.atlassian.plugin.classloader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.classworlds.uberjar.protocol.jar.NonLockingJarHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A class loader used to load classes and resources from a given plugin.
 */
public final class PluginClassLoader extends ClassLoader
{
    private static final String PLUGIN_INNER_JAR_PREFIX = "atlassian-plugins-innerjar";
    /**
     * the list of inner jars
     */
    private final List/*<File>*/ pluginInnerJars;
    /**
     * Mapping of <String> names (resource, or class name) to the <URL>s where the resource or class can be found.
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
            this.pluginInnerJars = new ArrayList();
            initializeFromJar(pluginFile, true);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Go through all entries in the given JAR, and recursively populate entryMappings by providing
     * resource or Class name to URL mappings. 
     *
     * @param file the file to scan
     * @throws IOException
     */
    private void initializeFromJar(File file, boolean isOuterJar) throws IOException
    {
        final JarFile jarFile = new JarFile(file);
        try
        {
            for (Enumeration entries = jarFile.entries(); entries.hasMoreElements();)
            {
                final JarEntry jarEntry = (JarEntry) entries.nextElement();
                if(isOuterJar && isInnerJarPath(jarEntry.getName()))
                    initialiseInnerJar(jarFile, jarEntry);
                else
                    addEntryMapping(jarEntry, file, isOuterJar);
            }
        }
        finally
        {
            jarFile.close();
        }
    }

    private boolean isInnerJarPath(String name){
         return name.startsWith("META-INF/lib/") && name.endsWith(".jar");
    }

    private void initialiseInnerJar(JarFile jarFile, JarEntry jarEntry) throws IOException
    {
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try
        {
            final File innerJarTmpFile = File.createTempFile(PLUGIN_INNER_JAR_PREFIX, ".jar");
            inputStream = jarFile.getInputStream(jarEntry);
            fileOutputStream = new FileOutputStream(innerJarTmpFile);
            IOUtils.copy(inputStream, fileOutputStream);
            initializeFromJar(innerJarTmpFile, false);
            pluginInnerJars.add(innerJarTmpFile);
        }
        finally
        {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(fileOutputStream);
        }
    }

    /**
     * This implementation of loadClass uses a child first delegation model rather than the standard parent first. If the
     * requested class cannot be found in this class loader, the parent class loader will be consulted via the standard
     * {@link ClassLoader#loadClass(String, boolean)} mechanism.
     *
     * @param name Class to load
     * @param resolve true to resolve all class dependencies when loaded
     * @return Class for the provided name
     * @throws ClassNotFoundException if the class cannot be found in this class loader or its parent
     */
    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        // First check if it's already been loaded
        Class c = findLoadedClass(name);
        if (c != null)
            return c;

        // If not, look inside the plugin before searching the parent.
        String path = name.replace('.', '/').concat(".class");
        if (isEntryInPlugin(path))
        {
            try
            {
                return loadClassFromPlugin(name, path);
            }
            catch (IOException e)
            {
                throw new ClassNotFoundException("Unable to load class [ " + name + " ] from PluginClassLoader", e);
            }
        }
        return super.loadClass(name, resolve);
    }

    /**
     * Load the named resource from this plugin. This implementation checks the plugin's contents first
     * then delegates to the system loaders.
     *
     * @param name the name of the resource.
     * @return the URL to the resource, <code>null</code> if the resource was not found.
     */
    public URL getResource(String name) {
        if (isEntryInPlugin(name))
        {
            return (URL) entryMappings.get(name);
        }
        else
        {
            return super.getResource(name);
        }
    }

    /**
     * Gets the resource from this classloader only
     *
     * @param name the name of the resource
     * @return the URL to the resource, <code>null</code> if the resource was not found
     */
    public URL getLocalResource(String name)
    {
        if (isEntryInPlugin(name))
            return getResource(name);
         else
            return null;
    }

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

    /**
     * This is based on part of the defineClass method in URLClassLoader (minus the package security checks).
     * See java.lang.ClassLoader.packages.
     * @param className to derive the package from
     */
    private void initializePackage(String className)
    {
        int i = className.lastIndexOf('.');
        if (i != -1)
        {
            String pkgname = className.substring(0, i);
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
        InputStream inputStream = null;
        try
        {
            URL resourceURL = (URL) entryMappings.get(path);
            inputStream = resourceURL.openStream();
            byte[] bytez = IOUtils.toByteArray(inputStream);
            initializePackage(className);
            return defineClass(className, bytez, 0, bytez.length);
        }
        finally
        {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private URL getUrlOfResourceInJar(String name, File jarFile)
    {
        try
        {
            URL url = new URL(new URL("jar:file:" + jarFile.getAbsolutePath() + "!/"), name, NonLockingJarHandler.getInstance());
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