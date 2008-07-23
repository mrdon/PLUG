package com.atlassian.plugin.osgi.deployer;

import junit.framework.TestCase;
import com.mockobjects.dynamic.Mock;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.util.Dictionary;
import java.util.Hashtable;

public class TestOsgiPlugin extends TestCase
{
    Mock mockBundle;
    OsgiPlugin plugin;

    @Override
    public void setUp()
    {
        mockBundle = new Mock(Bundle.class);
        Dictionary dict = new Hashtable();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        mockBundle.matchAndReturn("getHeaders", dict);

        plugin = new OsgiPlugin((Bundle) mockBundle.proxy());
    }

    @Override
    public void tearDown()
    {
        mockBundle = null;
        plugin = null;
    }
    public void testEnabled() {
        mockBundle.expectAndReturn("getState", Bundle.RESOLVED);
        mockBundle.expect("start");
        plugin.enable();
        mockBundle.verify();
    }
    public void testDisabled() {
        mockBundle.expectAndReturn("getState", Bundle.ACTIVE);
        mockBundle.expect("stop");
        plugin.disable();
        mockBundle.verify();
    }

    public void testClose() {
        mockBundle.expectAndReturn("getState", Bundle.ACTIVE);
        mockBundle.expect("uninstall");
        plugin.close();
        mockBundle.verify();
    }

    public void testisEnabled() {
        mockBundle.expectAndReturn("getState", Bundle.ACTIVE);
        assertTrue(plugin.isEnabled());
        mockBundle.verify();

        mockBundle.expectAndReturn("getState", Bundle.RESOLVED);
        assertTrue(!plugin.isEnabled());
        mockBundle.verify();
    }

}