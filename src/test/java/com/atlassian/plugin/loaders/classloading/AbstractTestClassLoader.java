package com.atlassian.plugin.loaders.classloading;

import com.atlassian.plugin.loaders.TestClassPathPluginLoader;
import com.atlassian.plugin.util.ClassLoaderUtils;
import junit.framework.TestCase;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public abstract class AbstractTestClassLoader extends TestCase
{
    protected File getPluginsDirectory() throws URISyntaxException
    {
        URL url = ClassLoaderUtils.getResource("plugins", TestClassPathPluginLoader.class);
        File pluginsDirectory = new File(new URI(url.toExternalForm()));
        return pluginsDirectory;
    }
}
