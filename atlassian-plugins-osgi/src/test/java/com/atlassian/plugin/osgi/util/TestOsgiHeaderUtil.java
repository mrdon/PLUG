package com.atlassian.plugin.osgi.util;

import com.atlassian.plugin.osgi.factory.transform.StubHostComponentRegistration;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.google.common.collect.Sets;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.*;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOsgiHeaderUtil extends TestCase 
{

    public void testFindReferredPackages() throws IOException
    {
        Set<String> foundPackages = OsgiHeaderUtil.findReferredPackageNames(new ArrayList<HostComponentRegistration>()
        {{
            add(new StubHostComponentRegistration(OsgiHeaderUtil.class));
        }});

        assertTrue(foundPackages.contains(HostComponentRegistration.class.getPackage().getName()));
    }

    public void testFindReferredPackagesWithVersion() throws IOException
    {
        Map<String, String> foundPackages = OsgiHeaderUtil.findReferredPackageVersions(new ArrayList<HostComponentRegistration>()
        {{
            add(new StubHostComponentRegistration(OsgiHeaderUtil.class));
        }}, Collections.singletonMap(HostComponentRegistration.class.getPackage().getName(), "1.0.45"));

        assertTrue(foundPackages.containsKey(HostComponentRegistration.class.getPackage().getName()));
        assertEquals(foundPackages.get(HostComponentRegistration.class.getPackage().getName()), "1.0.45");
    }

    public void testGetPluginKeyBundle()
    {
        Dictionary headers = new Hashtable();
        headers.put(Constants.BUNDLE_VERSION, "1.0");
        headers.put(Constants.BUNDLE_SYMBOLICNAME, "foo");
        
        Bundle bundle = mock(Bundle.class);
        when(bundle.getSymbolicName()).thenReturn("foo");
        when(bundle.getHeaders()).thenReturn(headers);

        assertEquals("foo-1.0", OsgiHeaderUtil.getPluginKey(bundle));

        headers.put(OsgiPlugin.ATLASSIAN_PLUGIN_KEY, "bar");
        assertEquals("bar", OsgiHeaderUtil.getPluginKey(bundle));
    }

    public void testGetPluginKeyManifest()
    {
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1.0");
        mf.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, "foo");

        assertEquals("foo-1.0", OsgiHeaderUtil.getPluginKey(mf));

        mf.getMainAttributes().putValue(OsgiPlugin.ATLASSIAN_PLUGIN_KEY, "bar");
        assertEquals("bar", OsgiHeaderUtil.getPluginKey(mf));
    }

    public void testGeneratePackageVersionString()
    {
        Map<String, String> input = new HashMap<String, String>();
        input.put("foo.bar", "1.2");
        input.put("foo.baz", null);

        String output = OsgiHeaderUtil.generatePackageVersionString(input);

        Set<String> set = Sets.newHashSet(output.split("[,]"));

        assertTrue(set.contains("foo.bar;version=1.2"));
        assertTrue(set.contains("foo.baz"));
        assertEquals(2, set.size());
    }

}
