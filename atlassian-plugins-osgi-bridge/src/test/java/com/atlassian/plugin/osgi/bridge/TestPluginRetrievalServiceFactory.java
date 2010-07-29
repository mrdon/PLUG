package com.atlassian.plugin.osgi.bridge;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import junit.framework.TestCase;
import org.osgi.framework.Bundle;

import java.util.Hashtable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TestPluginRetrievalServiceFactory extends TestCase
{
    public void testGetPlugin()
    {
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        Bundle bundle = mock(Bundle.class);
        Hashtable headers = new Hashtable();
        headers.put("Atlassian-Plugin-Key", "foo");
        when(bundle.getHeaders()).thenReturn(headers);
        Plugin plugin = mock(Plugin.class);
        when(pluginAccessor.getPlugin("foo")).thenReturn(plugin);

        PluginRetrievalServiceFactory factory = new PluginRetrievalServiceFactory(pluginAccessor);
        assertEquals(plugin, ((PluginRetrievalService)factory.getService(bundle, null)).getPlugin());
    }

    public void testGetPluginButFrameworkBundle()
    {
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        Bundle bundle = mock(Bundle.class);
        Hashtable headers = new Hashtable();
        headers.put("Atlassian-Plugin-Key", "foo");
        when(bundle.getHeaders()).thenReturn(headers);

        PluginRetrievalServiceFactory factory = new PluginRetrievalServiceFactory(pluginAccessor);
        assertNull(((PluginRetrievalService)factory.getService(bundle, null)).getPlugin());
    }
}
