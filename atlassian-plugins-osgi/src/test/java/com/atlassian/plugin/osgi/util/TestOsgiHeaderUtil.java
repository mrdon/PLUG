package com.atlassian.plugin.osgi.util;

import com.atlassian.plugin.osgi.factory.transform.StubHostComponentRegistration;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.impl.MockRegistration;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import junit.framework.TestCase;

import java.io.IOException;
import javax.servlet.ServletContext;
import javax.print.attribute.HashAttributeSet;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AttributeSet;
import java.util.*;
import java.util.jar.Manifest;

import org.twdata.pkgscanner.ExportPackage;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOsgiHeaderUtil extends TestCase 
{

    public void testFindReferredPackages() throws IOException
    {
        String foundPackages = OsgiHeaderUtil.findReferredPackages(new ArrayList<HostComponentRegistration>()
        {{
            add(new StubHostComponentRegistration(OsgiHeaderUtil.class));
        }});

        assertTrue(foundPackages.contains(HostComponentRegistration.class.getPackage().getName()));

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
}
