package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.PluginState;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.descriptors.RequiresRestart;
import com.atlassian.plugin.event.PluginEventManager;
import junit.framework.TestCase;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

public class TestOsgiPlugin extends TestCase
{
    private Bundle bundle;
    private OsgiPlugin plugin;
    private BundleContext bundleContext;
    private Dictionary dict;
    private OsgiPluginHelper helper;

    @Override
    public void setUp()
    {


        bundle = mock(Bundle.class);
        dict = new Hashtable();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        when(bundle.getHeaders()).thenReturn(dict);
        bundleContext = mock(BundleContext.class);
        when(bundle.getBundleContext()).thenReturn(bundleContext);

        helper = mock(OsgiPluginHelper.class);
        when(helper.getBundle()).thenReturn(bundle);

        plugin = new OsgiPlugin(mock(PluginEventManager.class), helper);
    }

    @Override
    public void tearDown()
    {
        bundle = null;
        plugin = null;
        bundleContext = null;
    }

    public void testEnabled() throws BundleException
    {
        when(bundle.getState()).thenReturn(Bundle.RESOLVED);
        plugin.enable();
        verify(bundle).start();
    }

    public void testDisabled() throws BundleException
    {
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);
        plugin.disable();
        verify(bundle).stop();
    }

    public void testDisabledOnNonDynamicPlugin() throws BundleException
    {
        plugin.addModuleDescriptor(new StaticModuleDescriptor());
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);
        plugin.disable();
        verify(bundle, never()).stop();
    }

    public void testUninstall() throws BundleException
    {
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);
        plugin.uninstall();
        assertEquals(plugin.getPluginState(), PluginState.UNINSTALLED);
    }

    @RequiresRestart
    public static class StaticModuleDescriptor extends AbstractModuleDescriptor
    {
        public Object getModule()
        {
            return null;
        }
    }


    public void testShouldHaveSpringContext() throws MalformedURLException
    {
        dict.put(OsgiPlugin.SPRING_CONTEXT, "*;timeout:=60");
        assertTrue(OsgiPlugin.shouldHaveSpringContext(bundle));

        dict.remove(OsgiPlugin.SPRING_CONTEXT);
        assertFalse(OsgiPlugin.shouldHaveSpringContext(bundle));
        when(bundle.getEntry("META-INF/spring/")).thenReturn(new URL("http://foo"));
        assertTrue(OsgiPlugin.shouldHaveSpringContext(bundle));


    }
}