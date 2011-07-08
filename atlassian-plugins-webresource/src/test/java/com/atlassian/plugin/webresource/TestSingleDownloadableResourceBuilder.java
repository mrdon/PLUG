package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.servlet.ServletContextFactory;
import junit.framework.TestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.mockito.Mockito.when;

public class TestSingleDownloadableResourceBuilder extends TestCase
{
    public static final String MODULE_KEY = "test.plugin.key:module";
    public static final String PLUGIN_KEY = "test.plugin.key";
    @Mock
    private PluginAccessor mockPluginAccessor;
    @Mock
    private ServletContextFactory mockServletContextFactory;
    @Mock
    ModuleDescriptor mockModuleDescriptor;

    SingleDownloadableResourceBuilder builder;
    Plugin plugin;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        
        MockitoAnnotations.initMocks(this);
        builder = new SingleDownloadableResourceBuilder(mockPluginAccessor, mockServletContextFactory);

        plugin = TestUtils.createTestPlugin(PLUGIN_KEY, "1.0");

        when(mockPluginAccessor.getEnabledPluginModule(MODULE_KEY)).thenReturn(mockModuleDescriptor);
        when(mockPluginAccessor.getPlugin(PLUGIN_KEY)).thenReturn(plugin);
    }

    @Override
    public void tearDown() throws Exception
    {
        mockPluginAccessor = null;
        mockServletContextFactory = null;
        
        super.tearDown();
    }
    
    public void testParseWithSimpleName() throws Exception
    {
        final ResourceLocation location = new ResourceLocation("", "mydownload.jpg", "download", "image/jpeg", "", Collections.<String, String>emptyMap());
        when(mockModuleDescriptor.getResourceLocation("download", "mydownload.jpg")).thenReturn(location);

        DownloadableResource resource = builder.parse("/download/resources/test.plugin.key:module/mydownload.jpg", Collections.<String, String>emptyMap());

        assertNotNull(resource);
        assertEquals(location.getContentType(), resource.getContentType());
    }

    public void testParseWithSlashesInName() throws Exception
    {
        final ResourceLocation location = new ResourceLocation("", "mydownload.swf", "download", "application/x-shockwave-flash", "", Collections.<String, String>emptyMap());
        when(mockModuleDescriptor.getResourceLocation("download", "path/to/mydownload.swf")).thenReturn(location);

        DownloadableResource resource = builder.parse("/download/resources/test.plugin.key:module/path/to/mydownload.swf", Collections.<String, String>emptyMap());

        assertNotNull(resource);
        assertEquals(location.getContentType(), resource.getContentType());
    }

    public void testRoundTrip() throws Exception
    {
        SinglePluginResource resource = new SinglePluginResource("foo.css", MODULE_KEY, false);
        String url = resource.getUrl();

        final ResourceLocation location = new ResourceLocation("", "foo.css", "download", "text/css", "", Collections.<String, String>emptyMap());
        when(mockModuleDescriptor.getResourceLocation("download", "foo.css")).thenReturn(location);

        DownloadableResource parsedResource = builder.parse(url, Collections.<String, String>emptyMap());

        assertNotNull(parsedResource);
        assertEquals(location.getContentType(), parsedResource.getContentType());
    }

    public void testParseInvlaidUrlThrowsException()
    {
        try
        {
            builder.parse("/download/resources/blah.png", Collections.<String, String>emptyMap());
            fail("Should have thrown exception for invalid url");
        }
        catch (UrlParseException e)
        {
            //expected
        }
    }
    
}
