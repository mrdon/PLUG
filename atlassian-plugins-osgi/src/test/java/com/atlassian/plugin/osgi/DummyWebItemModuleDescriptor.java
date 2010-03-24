package com.atlassian.plugin.osgi;

import java.io.InputStream;
import java.net.URL;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.atlassian.plugin.impl.AbstractPlugin;
import com.atlassian.plugin.web.DefaultWebInterfaceManager;
import com.atlassian.plugin.web.descriptors.DefaultWebItemModuleDescriptor;

public class DummyWebItemModuleDescriptor extends DefaultWebItemModuleDescriptor
{
    private final String key;

    public DummyWebItemModuleDescriptor()
    {
        super(new DefaultWebInterfaceManager());
        final Element e = DocumentHelper.createElement("somecrap");
        e.addAttribute("key", "foo");
        init(new MockPlugin(this.getClass().getName()), e);
        this.key = "somekey";
    }

    @Override
    public String getCompleteKey()
    {
        return "test.plugin:somekey";
    }

    @Override
    public String getPluginKey()
    {
        return "test.plugin";
    }

    @Override
    public String getKey()
    {
        return key;
    }

    private class MockPlugin extends AbstractPlugin
    {
        MockPlugin(final String key)
        {
            setKey(key);
            setName(key);
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
            return this.getClass().getClassLoader();
        }

        public URL getResource(final String path)
        {
            return null;
        }

        public InputStream getResourceAsStream(final String name)
        {
            return null;
        }
    }
}
