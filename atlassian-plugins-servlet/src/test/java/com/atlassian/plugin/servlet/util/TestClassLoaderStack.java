package com.atlassian.plugin.servlet.util;

import static com.atlassian.plugin.test.PluginTestUtils.FILTER_TEST_JAR;
import static com.atlassian.plugin.test.PluginTestUtils.getFileForResource;

import java.net.URISyntaxException;

import com.atlassian.plugin.classloader.PluginClassLoader;

import junit.framework.TestCase;

public class TestClassLoaderStack extends TestCase
{
    public void testThreadClassLoaderIsReplacedAndRestored() throws URISyntaxException
    {
        ClassLoader mainLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader pluginLoader = new PluginClassLoader(getFileForResource(FILTER_TEST_JAR));
        ClassLoader pluginLoader2 = new PluginClassLoader(getFileForResource(FILTER_TEST_JAR));

        ClassLoaderStack.push(pluginLoader);
        assertSame(pluginLoader, Thread.currentThread().getContextClassLoader());
        ClassLoaderStack.push(pluginLoader2);
        assertSame(pluginLoader2, Thread.currentThread().getContextClassLoader());
        ClassLoaderStack.pop();
        assertSame(pluginLoader, Thread.currentThread().getContextClassLoader());
        ClassLoaderStack.pop();
        assertSame(mainLoader, Thread.currentThread().getContextClassLoader());
    }
}
