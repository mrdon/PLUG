package com.atlassian.plugin.impl;

import com.atlassian.plugin.util.VersionStringComparator;

import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.net.URL;

import junit.framework.TestCase;

public class TestAbstractPlugin extends TestCase
{
    public void testCompareTo()
    {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("bar");

        // foo should be after bar
        assertTrue(p1.compareTo(p2) > 0);
        assertTrue(p2.compareTo(p1) < 0);
    }

    public void testCompareToOnVersionWithMilestone()
    {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        p1.getPluginInformation().setVersion("1.2.m2");
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");
        p2.getPluginInformation().setVersion("1.2.1");

        // v1.2.1 should be after v1.2.m2
        assertTrue(p1.compareTo(p2) < 0);
        assertTrue(p2.compareTo(p1) > 0);
    }

    public void testCompareToOnVersion()
    {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        p1.getPluginInformation().setVersion("3.4.1");
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");
        p2.getPluginInformation().setVersion("3.1.4");

        // v3.4.1 should be after v3.1.4
        assertTrue(p1.compareTo(p2) > 0);
        assertTrue(p2.compareTo(p1) < 0);
    }

    public void testCompareToWhenEqual()
    {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        p1.getPluginInformation().setVersion("3.1.4");
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");
        p2.getPluginInformation().setVersion("3.1.4");

        // Plugins are "equal" in order
        assertTrue(p1.compareTo(p2) == 0);
        assertTrue(p2.compareTo(p1) == 0);
    }

    public void testCompareToWithNullPluginInformation()
    {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        p1.setPluginInformation(null);
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");

        // p2 has default version (== "0.0")
        assertEquals("0.0", p2.getPluginInformation().getVersion());
        // p1 has null PluginInformation, but the compareTo() will "clean up" this to use version "0", considered equal to "0.0"
        assertTrue(p1.compareTo(p2) == 0);
        assertTrue(p2.compareTo(p1) == 0);
    }

    public void testCompareWithInvalidVersion() throws Exception
    {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        final String invalidVersion = "@$%^#";
        assertFalse(VersionStringComparator.isValidVersionString(invalidVersion));
        p1.getPluginInformation().setVersion(invalidVersion);
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");
        p2.getPluginInformation().setVersion("3.2");

        // The valid version should be after the invalid version
        assertEquals(-1, p1.compareTo(p2));
        assertEquals(1, p2.compareTo(p1));
    }

    public void testCompareWithBothVersionsInvalid() throws Exception
    {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        p1.getPluginInformation().setVersion("@$%^#");
        assertFalse(VersionStringComparator.isValidVersionString(p1.getPluginInformation().getVersion()));

        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");
        p2.getPluginInformation().setVersion("!!");

        // The plugins should sort equally
        assertEquals(0, p1.compareTo(p2));
        assertEquals(0, p2.compareTo(p1));
    }

    public void testCompareToWithNullKey()
    {
        final AbstractPlugin p1 = createAbstractPlugin();
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");

        // null should be before "foo"
        assertNull(p1.getKey());
        assertTrue(p1.compareTo(p2) < 0);
        assertTrue(p2.compareTo(p1) > 0);
    }

    public void testCompareToWithBothNullKeys()
    {
        final AbstractPlugin p1 = createAbstractPlugin();
        final AbstractPlugin p2 = createAbstractPlugin();

        assertNull(p1.getKey());
        assertTrue(p1.compareTo(p2) == 0);
        assertTrue(p2.compareTo(p1) == 0);
    }

    public void testGetNameReturnsKeyIfBlank()
    {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        assertEquals("foo", p1.getName());
    }

    public void testGetNameReturnsSetName()
    {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("key");
        p1.setI18nNameKey("i18n");
        p1.setName("name");
        assertEquals("name", p1.getName());
    }

    public void testGetNameReturnsBlankIfI18nNameKeySpecified()
    {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        p1.setI18nNameKey("i18n");
        assertTrue(StringUtils.isBlank(p1.getName()));
    }

    private AbstractPlugin createAbstractPlugin()
    {
        return new AbstractPlugin()
        {

            @Override
            public boolean isBundledPlugin()
            {
                return false;
            }

            public boolean isUninstallable()
            {
                return false;
            }

            public boolean isDeleteable()
            {
                return false;
            }

            public boolean isDynamicallyLoaded()
            {
                return false;
            }

            public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException
            {
                return null;
            }

            public ClassLoader getClassLoader()
            {
                return null;
            }

            public URL getResource(final String path)
            {
                return null;
            }

            public InputStream getResourceAsStream(final String name)
            {
                return null;
            }
        };
    }
}
