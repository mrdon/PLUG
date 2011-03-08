package com.atlassian.plugin.osgi.factory;

import junit.framework.TestCase;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.PluginArtifact;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.osgi.framework.Bundle;

import java.util.Hashtable;
import java.util.Dictionary;

public class TestOsgiPluginUninstalledHelper extends TestCase
{
    private String key = "key";
    private OsgiContainerManager mgr;
    private OsgiPluginUninstalledHelper helper;

    @Override
     protected void setUp() throws Exception
    {
        super.setUp();
        PluginArtifact pluginArtifact = mock(PluginArtifact.class);
        mgr = mock(OsgiContainerManager.class);
        helper = new OsgiPluginUninstalledHelper(key, mgr, pluginArtifact);
    }

    public void testInstall()
    {
        Dictionary dict = new Hashtable();
        dict.put(OsgiPlugin.ATLASSIAN_PLUGIN_KEY, key);
        Bundle bundle = mock(Bundle.class);
        when(bundle.getHeaders()).thenReturn(dict);
        when(bundle.getSymbolicName()).thenReturn(key);
        when(mgr.installBundle(null)).thenReturn(bundle);
        assertEquals(bundle, helper.install());

    }

    public void testInstallDifferentSymbolicName()
    {
        Dictionary dict = new Hashtable();
        Bundle bundle = mock(Bundle.class);
        when(bundle.getHeaders()).thenReturn(dict);
        when(bundle.getSymbolicName()).thenReturn("bar");
        when(mgr.installBundle(null)).thenReturn(bundle);
        try
        {
            helper.install();
            fail();
        }
        catch (IllegalArgumentException ex)
        {
            //test passed
        }
    }

    public void testInstallDifferentSymbolicNameButAltassianKeyFound()
    {
        Dictionary dict = new Hashtable();
        dict.put(OsgiPlugin.ATLASSIAN_PLUGIN_KEY, key);
        Bundle bundle = mock(Bundle.class);
        when(bundle.getHeaders()).thenReturn(dict);
        when(bundle.getSymbolicName()).thenReturn("bar");
        when(mgr.installBundle(null)).thenReturn(bundle);
        try
        {
            helper.install();
            // test passed
        }
        catch (IllegalArgumentException ex)
        {
            fail();
        }
    }
}
