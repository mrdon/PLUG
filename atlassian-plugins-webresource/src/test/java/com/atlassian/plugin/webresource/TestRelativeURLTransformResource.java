package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.webresource.SinglePluginResource.URL_PREFIX;
import static org.mockito.Mockito.when;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.servlet.DownloadableResource;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import junit.framework.TestCase;

public class TestRelativeURLTransformResource extends TestCase
{
    public static final String STATIC_PREFIX = "/test/s/en_GB/1/10/1000/_";
    public static final String MODULE_KEY = "atlassian:test-resource";
    public static final String TRANSFORM_PREFIX = STATIC_PREFIX + URL_PREFIX + PATH_SEPARATOR + MODULE_KEY + PATH_SEPARATOR;
    @Mock
    private DownloadableResource mockOriginalResource;
    @Mock
    private WebResourceUrlProvider mockWebResourceUrlProvider;
    @Mock
    private ModuleDescriptor mockModuleDescriptor;

    private Plugin testPlugin;
    private RelativeURLTransformResource transformResource;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        MockitoAnnotations.initMocks(this);
        transformResource = new RelativeURLTransformResource(mockWebResourceUrlProvider, mockModuleDescriptor, mockOriginalResource);

        testPlugin = TestUtils.createTestPlugin();
        when(mockModuleDescriptor.getPlugin()).thenReturn(testPlugin);
        when(mockModuleDescriptor.getCompleteKey()).thenReturn(MODULE_KEY);
        when(mockWebResourceUrlProvider.getStaticResourcePrefix("1", UrlMode.RELATIVE)).thenReturn(STATIC_PREFIX);
    }

    @Override
    public void tearDown() throws Exception
    {
        transformResource = null;
        mockWebResourceUrlProvider = null;
        mockModuleDescriptor = null;
        mockOriginalResource = null;

        super.tearDown();
    }

    public void testReplaceRelative()
    {
        assertTransformWorked(TRANSFORM_PREFIX + "../relative.png", "../relative.png");
    }

    public void testReplaceAbsoluteSkipped()
    {
        assertTransformWorked("/absolute.png", "/absolute.png");
    }

    public void testReplaceAbsoluteSkippedFQ()
    {
        assertTransformWorked("http://atlassian.com/test/absolute.png", "http://atlassian.com/test/absolute.png");
    }

    public void testReplaceAbsoluteHttpsSkipped()
    {
        assertTransformWorked("https://atlassian.com/test/absolute.png", "https://atlassian.com/test/absolute.png");
    }

    public void testReplaceDataUriSkipped()
    {
        assertTransformWorked("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABkAAAARCAYAAAA",
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABkAAAARCAYAAAA");
    }

    public void testReplaceRelativeWithFunkyCharacters()
    {
        assertTransformWorked("/f\u00F8tex/absolute.png", "/f\u00F8tex/absolute.png");
    }

    public void testReplaceRelativeWithFunkyCharactersFQ()
    {
        assertTransformWorked("http://www.f\u00F8tex.dk", "http://www.f\u00F8tex.dk");
    }

    public void testReplaceRelativeWithFunkyCharactersTxPrefix()
    {
        assertTransformWorked(TRANSFORM_PREFIX + "f\u00F8tex.png", "f\u00F8tex.png");
    }

    public void testReplaceRelativeWithNumbers()
    {
        assertTransformWorked("/1/absolute.png", "/1/absolute.png");
    }

    public void testReplaceRelativeWithNumbersFQ()
    {
        assertTransformWorked("http://192.168.1.1:8080/test/absolute.png", "http://192.168.1.1:8080/test/absolute.png");
    }

    public void testReplaceRelativeWithNumbersTxPrefix()
    {
        assertTransformWorked(TRANSFORM_PREFIX + "../relative1.png", "../relative1.png");
    }

    private void assertTransformWorked(final String expectedUrl, final String originalUrl)
    {
        assertTransformWorked("url(%s)", expectedUrl, originalUrl);
        assertTransformWorked("url( %s )", expectedUrl, originalUrl);
        assertTransformWorked("url (%s)", expectedUrl, originalUrl);
        assertTransformWorked("url(\"%s\")", expectedUrl, originalUrl);
        assertTransformWorked("url( \"%s\")", expectedUrl, originalUrl);
        assertTransformWorked("url (\"%s\")", expectedUrl, originalUrl);
        assertTransformWorked("url('%s')", expectedUrl, originalUrl);
        assertTransformWorked("url( '%s' )", expectedUrl, originalUrl);
        assertTransformWorked("url ('%s' )", expectedUrl, originalUrl);
    }

    private void assertTransformWorked(final String format, final String expectedUrl, final String originalUrl)
    {
        final String original = String.format(".test { blahblah %s }", String.format(format, originalUrl));
        final String expected = String.format(".test { blahblah %s }", String.format(format, expectedUrl));
        assertEquals(expected, transformResource.transform(original).toString());
    }
}
