package com.atlassian.plugin.osgi.factory;

import junit.framework.TestCase;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.PluginArtifact;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.osgi.framework.Bundle;

public class TestOsgiPluginUninstalledHelper extends TestCase
{
    private String key = "key";
    private PluginArtifact pluginArtifact;
    private OsgiContainerManager mgr;
    private OsgiPluginUninstalledHelper helper;

    @Override
     protected void setUp() throws Exception
    {
        super.setUp();
        pluginArtifact = mock(PluginArtifact.class);
        mgr = mock(OsgiContainerManager.class);
        helper = new OsgiPluginUninstalledHelper(key, mgr, pluginArtifact);
    }

    public void testInstall()
    {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getSymbolicName()).thenReturn(key);
        when(mgr.installBundle(null)).thenReturn(bundle);
        assertEquals(bundle, helper.install());

    }

    public void testInstallDifferentSymbolicName()
    {
        Bundle bundle = mock(Bundle.class);
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
}
