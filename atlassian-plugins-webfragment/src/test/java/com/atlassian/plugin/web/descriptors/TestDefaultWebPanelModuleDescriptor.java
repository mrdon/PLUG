package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.impl.AbstractPlugin;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.web.CopyingContextProvider;
import com.atlassian.plugin.web.NoOpContextProvider;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.model.EmbeddedTemplateWebPanel;
import com.atlassian.plugin.web.model.ResourceTemplateWebPanel;
import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestDefaultWebPanelModuleDescriptor extends TestCase
{
    private WebPanelModuleDescriptor descriptor;
    private final Plugin plugin = new MockPlugin(this.getClass().getName());
    private HostContainer hostContainer = mock(HostContainer.class);
    private WebInterfaceManager webInterfaceManager = new MockWebInterfaceManager();
    private ModuleFactory moduleClassFactory = mock(ModuleFactory.class);
    private PluginAccessor pluginAccessor = mock(PluginAccessor.class);
    private Map<String, Object> context = new HashMap<String, Object>();

    @Override
    protected void setUp() throws Exception
    {
        when(hostContainer.create(EmbeddedTemplateWebPanel.class)).thenReturn(new EmbeddedTemplateWebPanel(pluginAccessor));
        when(hostContainer.create(ResourceTemplateWebPanel.class)).thenReturn(new ResourceTemplateWebPanel(pluginAccessor));

        descriptor = new DefaultWebPanelModuleDescriptor(hostContainer, moduleClassFactory, webInterfaceManager);
    }

    public void testMissingResource() throws DocumentException, PluginParseException
    {
        final String webPanelXml = "<web-panel key=\"myPanel\" location=\"atl.header\"/>";
        try
        {
            descriptor.init(plugin, createElement(webPanelXml));
            fail("Descriptor should check for a resource with name 'view'");
        }
        catch (PluginParseException e)
        {
            // pass
        }
    }

    public void testMissingLocation() throws DocumentException, PluginParseException
    {
        final String webPanelXml = "<web-panel key=\"myPanel\">\n" +
                                    "  <resource name=\"view\" type=\"static\"><![CDATA[<b>Hello World!</b>]]></resource>\n" +
                                    "</web-panel>";
        try
        {
            descriptor.init(plugin, createElement(webPanelXml));
            fail("Descriptor should check for a location attribute");
        }
        catch (PluginParseException e)
        {
            // pass
        }
    }

    public void testGetEmbeddedTemplate() throws DocumentException, PluginParseException
    {
        final String webPanelXml = "<web-panel key=\"myPanel\" location=\"atl.header\">\n" +
                                    "  <resource name=\"view\" type=\"static\"><![CDATA[<b>Hello World!</b>]]></resource>\n" +
                                    "</web-panel>";
        descriptor.init(plugin, createElement(webPanelXml));

        assertEquals("atl.header", descriptor.getLocation());
        assertWebPanel(EmbeddedTemplateWebPanel.class);
        assertContextProvider(NoOpContextProvider.class);
        assertEquals("<b>Hello World!</b>", descriptor.getModule().getHtml(context));
    }

    public void testGetEmbeddedTemplateWithContextProvider() throws DocumentException, PluginParseException
    {
        final String webPanelXml = "<web-panel key=\"myPanel\" location=\"atl.header\">\n" +
                                    "  <resource name=\"view\" type=\"static\"><![CDATA[<b>Hello World!</b>]]></resource>\n" +
                                    "  <context-provider class=\"com.atlassian.plugin.web.descriptors.TestContextProvider\" />\n" +
                                    "</web-panel>";
        descriptor.init(plugin, createElement(webPanelXml));

        assertWebPanel(EmbeddedTemplateWebPanel.class);
        assertContextProvider(TestContextProvider.class);
    }

    public void testGetResourceTemplate() throws DocumentException, PluginParseException
    {
        final String webPanelXml = "<web-panel key=\"myPanel\" location=\"atl.header\">\n" +
                                    "  <resource name=\"view\" type=\"static\" location=\"ResourceTemplateWebPanelTest.txt\"/>\n" +
                                    "</web-panel>";

        descriptor.init(plugin, createElement(webPanelXml));

        assertEquals("atl.header", descriptor.getLocation());
        assertContextProvider(NoOpContextProvider.class);
        assertWebPanel(ResourceTemplateWebPanel.class);

        String html = descriptor.getModule().getHtml(context);
        assertTrue(html.startsWith("This file is used as web panel contents in unit tests"));
    }

    public void testGetResourceTemplateWithContextProvider() throws DocumentException, PluginParseException
    {
        final String webPanelXml = "<web-panel key=\"myPanel\" location=\"atl.header\">\n" +
                                    "  <resource name=\"view\" type=\"static\" location=\"ResourceTemplateWebPanelTest.txt\"/>\n" +
                                    "  <context-provider class=\"com.atlassian.plugin.web.descriptors.TestContextProvider\" />\n" +
                                    "</web-panel>";
        descriptor.init(plugin, createElement(webPanelXml));

        assertWebPanel(ResourceTemplateWebPanel.class);
        assertContextProvider(TestContextProvider.class);
    }

    private void assertWebPanel(Class webPanelClass)
    {
        assertEquals(webPanelClass, descriptor.getModule().getClass());
    }

    private void assertContextProvider(Class expectedContextProviderClass)
    {
        CopyingContextProvider contextProvider = (CopyingContextProvider) descriptor.getContextProvider();
        assertEquals(expectedContextProviderClass, contextProvider.getDelegate().getClass());
    }

    private Element createElement(final String childElement) throws DocumentException
    {
        final Document document = DocumentHelper.parseText(childElement);
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
