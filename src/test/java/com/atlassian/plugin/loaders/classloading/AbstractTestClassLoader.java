package com.atlassian.plugin.loaders.classloading;

import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.plugin.loaders.TestClassPathPluginLoader;
import junit.framework.TestCase;

import java.io.File;
import java.net.URL;

public abstract class AbstractTestClassLoader extends TestCase
{
    protected File getPluginsDirectory()
    {
        URL url = ClassLoaderUtils.getResource("plugins", TestClassPathPluginLoader.class);
        String path = url.toExternalForm().substring(5);
    	path = path.replace('/', File.separatorChar);
        File pluginsDirectory = new File(path);
        return pluginsDirectory;
    }
}
