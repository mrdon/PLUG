package com.atlassian.plugin.manager;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.PluginState;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.plugin.impl.StaticPlugin;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;

public class TestPluginEnabler extends TestCase
{
    private Mock mockAccessor;
    private Mock mockController;
    private PluginEnabler enabler;

    @Override
    public void setUp()
    {
        mockAccessor = new Mock(PluginAccessor.class);
        mockController = new Mock(PluginController.class);
        enabler = new PluginEnabler((PluginAccessor) mockAccessor.proxy(), (PluginController) mockController.proxy());
    }

    public void testEnable()
    {
        Plugin plugin = new MyPlugin("foo");

        enabler.enable(Arrays.asList(plugin));
        assertEquals(PluginState.ENABLED, plugin.getPluginState());
    }

    public void testEnableWithCustomTimeout()
    {

        Plugin plugin = new MyPlugin("foo") {
            @Override
            protected PluginState enableInternal() throws PluginException
            {
                return PluginState.ENABLING;
            }
        };

        try
        {
            System.setProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT, "1");
            long start = System.currentTimeMillis();
            enabler = new PluginEnabler(mock(PluginAccessor.class), mock(PluginController.class));
            enabler.enable(Arrays.asList(plugin));
            long end = System.currentTimeMillis();
            assertTrue(end - start < 5000);
            assertEquals(PluginState.ENABLING, plugin.getPluginState());
        }
        finally
        {
            System.clearProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT);
        }
    }

    public void testRecursiveEnable()
    {
        Plugin plugin = new MyPlugin("foo", "foo2");
        Plugin plugin2 = new MyPlugin("foo2", "foo3");
        Plugin plugin3 = new MyPlugin("foo3");

        enabler.enable(Arrays.asList(plugin, plugin2, plugin3));
        assertEquals(PluginState.ENABLED, plugin.getPluginState());
        assertEquals(PluginState.ENABLED, plugin2.getPluginState());
        assertEquals(PluginState.ENABLED, plugin3.getPluginState());
    }

    public void testRecursiveCircular()
    {
        Plugin plugin = new MyPlugin("foo", "foo2");
        Plugin plugin2 = new MyPlugin("foo2", "foo3");
        Plugin plugin3 = new MyPlugin("foo3", "foo");

        enabler.enable(Arrays.asList(plugin, plugin2, plugin3));
        assertEquals(PluginState.ENABLED, plugin.getPluginState());
        assertEquals(PluginState.ENABLED, plugin2.getPluginState());
        assertEquals(PluginState.ENABLED, plugin3.getPluginState());
    }

    public static class MyPlugin extends StaticPlugin
    {
        private final Set<String> deps;

        public MyPlugin(String key, String... deps)
        {
            setKey(key);
            this.deps = new HashSet<String>(Arrays.asList(deps));
            setPluginState(PluginState.DISABLED);
        }

        @Override
        public Set<String> getRequiredPlugins()
        {
            return deps;
        }
    }
}