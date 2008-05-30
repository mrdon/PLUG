package com.atlassian.plugin.classloader;

import junit.framework.TestCase;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.Plugin;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedList;

/**
 *
 */
public class TestPluginsClassLoader extends TestCase
{
    private static final String TEST_RESOURCE = "log4j.properties";

    private PluginsClassLoader pluginsClassLoader;
    private Mock mockPluginAccessor;
    private Mock mockPlugin;
    private static final String PLUGIN_KEY = "aPluginKey";
    private static final String TEST_CLASS = "java.lang.String";

    protected void setUp() throws Exception
    {
        mockPluginAccessor = new Mock(PluginAccessor.class);
        pluginsClassLoader = new PluginsClassLoader((PluginAccessor) mockPluginAccessor.proxy());

        mockPlugin = new Mock(Plugin.class);
    }

    protected void tearDown() throws Exception
    {
        mockPluginAccessor.verify();
        mockPlugin.verify();

        mockPluginAccessor = null;
        pluginsClassLoader = null;
    }

    public void testFindResourceWhenIndexed()
    {
        final StubClassLoader stubClassLoader = new StubClassLoader();
        loadPluginResource(stubClassLoader);
        assertTrue(stubClassLoader.getFindResourceNames().contains(TEST_RESOURCE));
        stubClassLoader.clear();

        mockPlugin.expectAndReturn("getKey", PLUGIN_KEY);
        mockPluginAccessor.matchAndReturn("isPluginEnabled", C.args(C.eq(PLUGIN_KEY)), Boolean.TRUE);
        mockPlugin.expectAndReturn("getClassLoader", stubClassLoader);
        pluginsClassLoader.findResource(TEST_RESOURCE);

        assertTrue(stubClassLoader.getFindResourceNames().contains(TEST_RESOURCE));
    }

    public void testFindResourceWhenNotIndexed()
    {
        final StubClassLoader stubClassLoader = new StubClassLoader();
        loadPluginResource(stubClassLoader);

        assertTrue(stubClassLoader.getFindResourceNames().contains(TEST_RESOURCE));
    }

    public void testFindResourceWhenIndexedAndPluginDisabled()
    {
        final StubClassLoader stubClassLoader = new StubClassLoader();
        loadPluginResource(stubClassLoader);
        assertTrue(stubClassLoader.getFindResourceNames().contains(TEST_RESOURCE));
        stubClassLoader.clear();

        mockPluginAccessor.expectAndReturn("getEnabledPlugins", Collections.EMPTY_LIST);
        mockPlugin.expectAndReturn("getKey", PLUGIN_KEY);
        mockPluginAccessor.matchAndReturn("isPluginEnabled", C.args(C.eq(PLUGIN_KEY)), Boolean.FALSE);
        pluginsClassLoader.findResource(TEST_RESOURCE);

        assertFalse(stubClassLoader.getFindResourceNames().contains(TEST_RESOURCE));
    }

    public void testFindClassWhenIndexed() throws Exception
    {
        final StubClassLoader stubClassLoader = new StubClassLoader();
        loadPluginClass(stubClassLoader);
        assertTrue(stubClassLoader.getFindClassNames().contains(TEST_CLASS));
        stubClassLoader.clear();

        mockPlugin.expectAndReturn("getKey", PLUGIN_KEY);
        mockPluginAccessor.matchAndReturn("isPluginEnabled", C.args(C.eq(PLUGIN_KEY)), Boolean.TRUE);
        mockPlugin.expectAndReturn("getClassLoader", stubClassLoader);
        pluginsClassLoader.findClass(TEST_CLASS);

        assertTrue(stubClassLoader.getFindClassNames().contains(TEST_CLASS));
    }

    public void testFindClassWhenNotIndexed() throws Exception
    {
        final StubClassLoader stubClassLoader = new StubClassLoader();
        loadPluginClass(stubClassLoader);

        assertTrue(stubClassLoader.getFindClassNames().contains(TEST_CLASS));
    }

    public void testFindClassWhenIndexedAndPluginDisabled() throws Exception
    {
        final StubClassLoader stubClassLoader = new StubClassLoader();
        loadPluginClass(stubClassLoader);
        assertTrue(stubClassLoader.getFindClassNames().contains(TEST_CLASS));
        stubClassLoader.clear();

        mockPluginAccessor.expectAndReturn("getEnabledPlugins", Collections.EMPTY_LIST);
        mockPlugin.expectAndReturn("getKey", PLUGIN_KEY);
        mockPluginAccessor.matchAndReturn("isPluginEnabled", C.args(C.eq(PLUGIN_KEY)), Boolean.FALSE);
        try
        {
            pluginsClassLoader.findClass(TEST_CLASS);
            fail("Plugin is disabled so its ClassLoader should throw ClassNotFoundException");
        }
        catch (ClassNotFoundException e)
        {
            // good
        }
    }

    private void loadPluginResource(ClassLoader stubClassLoader)
    {
        mockPluginAccessor.expectAndReturn("getEnabledPlugins", Collections.singleton(mockPlugin.proxy()));
        mockPlugin.expectAndReturn("getClassLoader", stubClassLoader);
        pluginsClassLoader.findResource(TEST_RESOURCE);
    }

    private void loadPluginClass(ClassLoader stubClassLoader) throws ClassNotFoundException
    {
        mockPluginAccessor.expectAndReturn("getEnabledPlugins", Collections.singleton(mockPlugin.proxy()));
        mockPlugin.expectAndReturn("getClassLoader", stubClassLoader);
        pluginsClassLoader.findClass(TEST_CLASS);
    }

    private static final class StubClassLoader extends AbstractClassLoader
    {
        private final Collection findResourceNames = new LinkedList();

        public Collection getFindClassNames()
        {
            return findClassNames;
        }

        private final Collection findClassNames = new LinkedList();

        public StubClassLoader()
        {
            super(null); // no parent classloader needed for tests
        }

        protected URL findResource(String name)
        {
            findResourceNames.add(name);
            try
            {
                return new URL("file://"+name);
            }
            catch (MalformedURLException e)
            {
                // ignore
                return null;
            }
        }

        /**
         * override the default behavior to bypass the system class loader
         * for tests
         */
        public Class loadClass(String name) throws ClassNotFoundException
        {
            return findClass(name);
        }

        protected Class findClass(String className) throws ClassNotFoundException
        {
            findClassNames.add(className);
            return String.class;
        }

        public Collection getFindResourceNames()
        {
            return findResourceNames;
        }

        public void clear() {
            findResourceNames.clear();
            findClassNames.clear();
        }
    }
}
