package com.atlassian.plugin.loaders.classloading;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.MalformedURLException;

public class DirectoryClassLoader extends PluginsClassLoader
{
    private File directory;
    private JarClassLoader[] loaders;

    public DirectoryClassLoader(File directory, ClassLoader parent)
    {
        super(parent);
        this.directory = directory;


        File[] files = directory.listFiles(new FilenameFilter()
        {
            public boolean accept(File file, String s)
            {
                return s.endsWith(".jar");
            }
        });

        loaders = new JarClassLoader[files.length];
        for (int i = 0; i < files.length; i++)
        {
            loaders[i] = new JarClassLoader(files[i], this);
        }
    }

    protected URL getDataURL(String name, byte[] data) throws MalformedURLException
    {
        return null;
    }

    protected byte[] getFile(String path)
    {
        return new byte[0];
    }

    public Object clone()
    {
        DirectoryClassLoader loader = new DirectoryClassLoader(directory, getParent());
        loader.packages = packages;
        return loader;
    }
}