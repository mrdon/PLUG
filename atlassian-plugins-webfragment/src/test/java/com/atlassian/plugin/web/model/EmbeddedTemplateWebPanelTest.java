package com.atlassian.plugin.web.model;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.web.NoOpContextProvider;
import com.atlassian.plugin.web.renderer.WebPanelRenderer;
import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmbeddedTemplateWebPanelTest extends TestCase
{
    public void testGetHtml()
    {
        final PluginAccessor accessorMock = mock(PluginAccessor.class);
        when(accessorMock.getEnabledModulesByClass(WebPanelRenderer.class)).thenReturn(Collections.<WebPanelRenderer>emptyList());

        final EmbeddedTemplateWebPanel webPanel = new EmbeddedTemplateWebPanel(accessorMock);
        webPanel.setResourceType("static");
        webPanel.setTemplateBody("body");
        webPanel.setContextProvider(new NoOpContextProvider());

        assertEquals("body", webPanel.getHtml(Collections.<String, Object>emptyMap()));
    }

    public void testUnsupportedResourceType()
    {
        final PluginAccessor accessorMock = mock(PluginAccessor.class);
        final WebPanelRenderer renderer = mock(WebPanelRenderer.class);
        when(renderer.getResourceType()).thenReturn("velocity");
        when(accessorMock.getEnabledModulesByClass(WebPanelRenderer.class)).thenReturn(ImmutableList.of(renderer));

        final EmbeddedTemplateWebPanel webPanel = new EmbeddedTemplateWebPanel(accessorMock);
        webPanel.setResourceType("unsupported-type");
        webPanel.setTemplateBody("body");
        webPanel.setContextProvider(new NoOpContextProvider());

        final String result = webPanel.getHtml(Collections.<String, Object>emptyMap());
        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("error"));
    }
}
