package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformerModuleDescriptor;
import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestWebResourceTransformation extends TestCase
{
    public void testMatches() throws DocumentException {
        WebResourceTransformation trans = new WebResourceTransformation(DocumentHelper.parseText(
                "<transformation extension=\"js\">\n" +
                    "<transformer key=\"foo\" />\n" +
                "</transformation>").getRootElement());
        ResourceLocation loc = mock(ResourceLocation.class);
        when(loc.getName()).thenReturn("foo.js");
        assertTrue(trans.matches(loc));
    }

    public void testNotMatches() throws DocumentException {
        WebResourceTransformation trans = new WebResourceTransformation(DocumentHelper.parseText(
                "<transformation extension=\"js\">\n" +
                    "<transformer key=\"foo\" />\n" +
                "</transformation>").getRootElement());
        ResourceLocation loc = mock(ResourceLocation.class);
        when(loc.getName()).thenReturn("foo.cs");
        assertFalse(trans.matches(loc));
    }

    public void testNoExtension() throws DocumentException {
        try
        {
            new WebResourceTransformation(DocumentHelper.parseText(
                    "<transformation>\n" +
                            "<transformer key=\"foo\" />\n" +
                            "</transformation>").getRootElement());
            fail("Should have forced extension");
        }
        catch (IllegalArgumentException ex)
        {
            // pass
        }
    }

    public void testTransformDownloadableResource() throws DocumentException {
        Element element = DocumentHelper.parseText(
                "<transformation extension=\"js\">\n" +
                        "<transformer key=\"foo\" />\n" +
                        "</transformation>").getRootElement();
        WebResourceTransformation trans = new WebResourceTransformation(element);
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        WebResourceTransformerModuleDescriptor descriptor = mock(WebResourceTransformerModuleDescriptor.class);
        when(descriptor.getKey()).thenReturn("foo");
        WebResourceTransformer transformer = mock(WebResourceTransformer.class);
        when(descriptor.getModule()).thenReturn(transformer);
        when(pluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceTransformerModuleDescriptor.class)).thenReturn(
                Arrays.asList(descriptor));
        ResourceLocation loc = mock(ResourceLocation.class);
        when(loc.getName()).thenReturn("foo.js");

        DownloadableResource originalResource = mock(DownloadableResource.class);
        DownloadableResource transResource = mock(DownloadableResource.class);
        when(transformer.transform(element.element("transformer"), loc, "", originalResource)).thenReturn(transResource);

        DownloadableResource testResource = trans.transformDownloadableResource(pluginAccessor, originalResource, loc, "");
        assertEquals(transResource, testResource);
    }

}
