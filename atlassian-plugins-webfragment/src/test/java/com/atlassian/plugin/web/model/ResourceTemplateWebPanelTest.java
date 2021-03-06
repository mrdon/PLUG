package com.atlassian.plugin.web.model;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.web.renderer.WebPanelRenderer;
import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceTemplateWebPanelTest extends TestCase
{
    public void testGetHtml()
    {
        final PluginAccessor accessorMock = mock(PluginAccessor.class);
        when(accessorMock.getEnabledModulesByClass(WebPanelRenderer.class)).thenReturn(Collections.<WebPanelRenderer>emptyList());
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getClassLoader()).thenReturn(this.getClass().getClassLoader());

        final ResourceTemplateWebPanel webPanel = new ResourceTemplateWebPanel(accessorMock);
        webPanel.setPlugin(plugin);
        webPanel.setResourceType("static");
        webPanel.setResourceFilename("ResourceTemplateWebPanelTest.txt");

        assertTrue(webPanel.getHtml(Collections.<String, Object>emptyMap())
            .contains("This file is used as web panel contents in unit tests."));
    }

    public void testUnsupportedResourceType()
    {
        final PluginAccessor accessorMock = mock(PluginAccessor.class);
        final WebPanelRenderer renderer = mock(WebPanelRenderer.class);
        when(renderer.getResourceType()).thenReturn("velocity");
        when(accessorMock.getEnabledModulesByClass(WebPanelRenderer.class)).thenReturn(ImmutableList.of(renderer));
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getClassLoader()).thenReturn(this.getClass().getClassLoader());

        final ResourceTemplateWebPanel webPanel = new ResourceTemplateWebPanel(accessorMock);
        webPanel.setPlugin(plugin);
        webPanel.setResourceType("unsupported-type");
        webPanel.setResourceFilename("ResourceTemplateWebPanelTest.txt");

        final String result = webPanel.getHtml(Collections.<String, Object>emptyMap());
        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("error"));
    }
}
