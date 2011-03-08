package com.atlassian.plugin.web.descriptors;

import java.io.InputStream;
import java.net.URL;

import junit.framework.TestCase;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.impl.AbstractPlugin;

public class TestDefaultWebItemModuleDescriptor extends TestCase
{
    private WebItemModuleDescriptor descriptor;
    private final Plugin plugin = new MockPlugin(this.getClass().getName());

    @Override
    protected void setUp() throws Exception
    {
        descriptor = new DefaultWebItemModuleDescriptor(new MockWebInterfaceManager());
    }

    public void testGetStyleClass() throws DocumentException, PluginParseException
    {
        final String className = "testClass";
        final String styleClass = "<styleClass>" + className + "</styleClass>";

        final Element element = createElement(styleClass);
        descriptor.init(plugin, element);

        assertEquals(className, descriptor.getStyleClass());
    }

    public void testGetStyleClassTrimmed() throws DocumentException, PluginParseException
    {
        final String className = "testClass";
        final String styleClass = "<styleClass>   " + className + "   </styleClass>";

        final Element element = createElement(styleClass);
        descriptor.init(plugin, element);

        assertEquals(className, descriptor.getStyleClass());
    }

    public void testGetStyleClassSpaceSeparated() throws DocumentException, PluginParseException
    {
        final String className = "testClass testClass2";
        final String styleClass = "<styleClass>" + className + "</styleClass>";

        final Element element = createElement(styleClass);
        descriptor.init(plugin, element);

        assertEquals(className, descriptor.getStyleClass());
    }

    public void testGetStyleClassEmpty() throws DocumentException, PluginParseException
    {
        final String styleClass = "<styleClass></styleClass>";

        final Element element = createElement(styleClass);
        descriptor.init(plugin, element);

        assertEquals("", descriptor.getStyleClass());
    }

    public void testGetStyleClassNone() throws DocumentException, PluginParseException
    {
        final String styleClass = "";

        final Element element = createElement(styleClass);
        descriptor.init(plugin, element);

        assertNotNull(descriptor.getStyleClass());
        assertEquals("", descriptor.getStyleClass());
    }

    private Element createElement(final String childElement) throws DocumentException
    {
        final String rootElement = "<root key=\"key\">" + childElement + "</root>";
        final Document document = DocumentHelper.parseText(rootElement);
        final Element element = document.getRootElement();
        return element;
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
