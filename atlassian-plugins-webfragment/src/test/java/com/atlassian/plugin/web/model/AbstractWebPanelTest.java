package com.atlassian.plugin.web.model;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.web.renderer.RendererException;
import com.atlassian.plugin.web.renderer.StaticWebPanelRenderer;
import com.atlassian.plugin.web.renderer.WebPanelRenderer;
import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;

public class AbstractWebPanelTest extends TestCase
{
    public void testStaticRenderer()
    {
        AbstractWebPanel panel = new AbstractWebPanel(null)
        {
            public String getHtml(Map<String, Object> context)
            {
                final WebPanelRenderer renderer = getRenderer();
                assertEquals(renderer, StaticWebPanelRenderer.RENDERER);
                return null;
            }
        };

        panel.setResourceType(StaticWebPanelRenderer.RESOURCE_TYPE);
        panel.getHtml(null);
    }

    public void testUnsupportedRendererType()
    {
        final PluginAccessor accessorMock = mock(PluginAccessor.class);
        when(accessorMock.getEnabledModulesByClass(WebPanelRenderer.class)).thenReturn(Collections.<WebPanelRenderer>emptyList());

        AbstractWebPanel panel = new AbstractWebPanel(accessorMock)
        {
            public String getHtml(Map<String, Object> context)
            {
                try
                {
                    getRenderer();
                    fail();
                }
                catch (RendererException re)
                {
                    // expected
                }
                return null;
            }
        };

        panel.setResourceType("unsupported-type");
        panel.getHtml(null);
    }

    public void testSupportedRendererType()
    {
        final PluginAccessor accessorMock = mock(PluginAccessor.class);
        final WebPanelRenderer velocityRenderer = mock(WebPanelRenderer.class);
        when(velocityRenderer.getResourceType()).thenReturn("velocity");
        final WebPanelRenderer unsupportedRenderer = mock(WebPanelRenderer.class);
        when(unsupportedRenderer.getResourceType()).thenReturn("unsupported-type");
        when(accessorMock.getEnabledModulesByClass(WebPanelRenderer.class)).thenReturn(ImmutableList.of(unsupportedRenderer, velocityRenderer));

        AbstractWebPanel panel = new AbstractWebPanel(accessorMock)
        {
            public String getHtml(Map<String, Object> context)
            {
                final WebPanelRenderer webPanelRenderer = getRenderer();
                assertNotNull(webPanelRenderer);
                assertEquals(velocityRenderer, webPanelRenderer);
                return null;
            }
        };

        panel.setResourceType("velocity");
        panel.getHtml(null);
    }
}
