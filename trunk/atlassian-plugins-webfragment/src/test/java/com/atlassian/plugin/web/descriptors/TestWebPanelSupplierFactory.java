package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.web.model.EmbeddedTemplateWebPanel;
import com.atlassian.plugin.web.model.ResourceTemplateWebPanel;
import com.atlassian.plugin.web.model.WebPanel;
import com.google.common.base.Supplier;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestWebPanelSupplierFactory extends TestCase
{
    private WebPanelSupplierFactory webPanelSupplierFactory;

    private HostContainer hostContainer = mock(HostContainer.class);
    private ModuleFactory moduleFactory = mock(ModuleFactory.class);
    private WebPanelModuleDescriptor descriptor = mock(WebPanelModuleDescriptor.class);
    private PluginAccessor pluginAccessor = mock(PluginAccessor.class);

    @Override
    protected void setUp() throws Exception
    {
        webPanelSupplierFactory = new WebPanelSupplierFactory(descriptor, hostContainer, moduleFactory);
    }

    public void testEmbeddedTemplateWebPanel() throws Exception
    {
        ResourceDescriptor resourceDescriptor = mock(ResourceDescriptor.class);
        when(resourceDescriptor.getName()).thenReturn("view");
        when(resourceDescriptor.getType()).thenReturn("static");
        when(resourceDescriptor.getLocation()).thenReturn(null);
        when(resourceDescriptor.getContent()).thenReturn("Foo");
        List<ResourceDescriptor> resourceDescriptors = Arrays.asList(resourceDescriptor);
        when(descriptor.getResourceDescriptors()).thenReturn(resourceDescriptors);
        when(hostContainer.create(EmbeddedTemplateWebPanel.class)).thenReturn(new EmbeddedTemplateWebPanel(pluginAccessor));

        Supplier<WebPanel> panelSupplier = webPanelSupplierFactory.build(null);
        WebPanel panel = panelSupplier.get();
        assertEquals(EmbeddedTemplateWebPanel.class, panel.getClass());
    }

    public void testResourceTemplateWebPanel() throws Exception
    {
        ResourceDescriptor resourceDescriptor = mock(ResourceDescriptor.class);
        when(resourceDescriptor.getName()).thenReturn("view");
        when(resourceDescriptor.getType()).thenReturn("static");
        when(resourceDescriptor.getLocation()).thenReturn("some-filename.vm");
        List<ResourceDescriptor> resourceDescriptors = Arrays.asList(resourceDescriptor);
        when(descriptor.getResourceDescriptors()).thenReturn(resourceDescriptors);
        when(hostContainer.create(ResourceTemplateWebPanel.class)).thenReturn(new ResourceTemplateWebPanel(pluginAccessor));

        Supplier<WebPanel> panelSupplier = webPanelSupplierFactory.build(null);
        WebPanel panel = panelSupplier.get();
        assertEquals(ResourceTemplateWebPanel.class, panel.getClass());
    }

    public void testSuppliedWebPanel() throws Exception
    {
        String moduleClassName = "com.atlassian.plugin.web.descriptors.MockWebPanel";
        when(moduleFactory.createModule(moduleClassName, descriptor)).thenReturn(new MockWebPanel());

        Supplier<WebPanel> panelSupplier = webPanelSupplierFactory.build(moduleClassName);
        WebPanel panel = panelSupplier.get();
        assertEquals(MockWebPanel.class, panel.getClass());
    }
}
