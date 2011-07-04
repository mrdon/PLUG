package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.servlet.DownloadableResource;
import junit.framework.TestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.webresource.SinglePluginResource.URL_PREFIX;
import static org.mockito.Mockito.when;

public class TestRelativeURLTransformResource extends TestCase
{
    public static final String BASE_URL = "/test";
    public static final String MODULE_KEY = "atlassian:test-resource";
    public static final String TRANSFORM_PREFIX = BASE_URL + URL_PREFIX + PATH_SEPARATOR + MODULE_KEY + PATH_SEPARATOR;
    @Mock
    private DownloadableResource mockOriginalResource;
    @Mock
    private WebResourceIntegration mockWebResourceIntegration;
    @Mock
    private ModuleDescriptor mockModuleDescriptor;

    private RelativeURLTransformResource transformResource;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        MockitoAnnotations.initMocks(this);
        transformResource = new RelativeURLTransformResource(mockWebResourceIntegration, mockModuleDescriptor, mockOriginalResource);

        when(mockWebResourceIntegration.getBaseUrl()).thenReturn(BASE_URL);
        when(mockModuleDescriptor.getCompleteKey()).thenReturn(MODULE_KEY);
    }

    @Override
    public void tearDown() throws Exception
    {
        transformResource = null;
        mockWebResourceIntegration = null;
        mockModuleDescriptor = null;
        mockOriginalResource = null;

        super.tearDown();
    }

    public void testReplaceRelative()
    {
        String original =
                ".test {\n" +
                "    background-image: url(\"../relative.png\")\n" +
                "}";

        String result = transformResource.transform(original);

        String expected =
                ".test {\n" +
                "    background-image: url(\"" + TRANSFORM_PREFIX + "../relative.png\")\n" +
                "}";

        assertEquals(expected, result);
    }

    public void testReplaceRelativeWithSingleQuotes()
    {
        String original =
                ".test {\n" +
                "    background-image: url(\'../relative.png\')\n" +
                "}";

        String result = transformResource.transform(original);

        String expected =
                ".test {\n" +
                "    background-image: url(\'" + TRANSFORM_PREFIX + "../relative.png\')\n" +
                "}";

        assertEquals(expected, result);
    }

    public void testReplaceRelativeWithNoQuotes()
    {
        String original =
                ".test {\n" +
                "    background-image: url(../relative.png)\n" +
                "}";

        String result = transformResource.transform(original);

        String expected =
                ".test {\n" +
                "    background-image: url(" + TRANSFORM_PREFIX + "../relative.png)\n" +
                "}";

        assertEquals(expected, result);
    }

    public void testReplaceAbsoluteSkipped()
    {
        String original =
                ".test {\n" +
                "    background-image: url(\"/absolute.png\")\n" +
                "}\n" +
                ".sample {\n" +
                "    background-image: url(\"http://atlassian.com/test/absolute.png\")\n" +
                "}\n";

        String result = transformResource.transform(original);

        // No change
        assertEquals(original, result);
    }

    public void testReplaceDataUriSkipped()
    {
        String original =
                ".test {\n" +
                "    background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABkAAAARCAYAAAAougcOAAAC7mlDQ1BJQ0MgUHJvZmlsZQAAeAGFVM9rE0EU/jZuqdAiCFprDrJ4kCJJWatoRdQ2/RFiawzbH7ZFkGQzSdZuNuvuJrWliOTi0SreRe2hB/+AHnrwZC9KhVpFKN6rKGKhFy3xzW5MtqXqwM5+8943731vdt8ADXLSNPWABOQNx1KiEWlsfEJq/IgAjqIJQTQlVdvsTiQGQYNz+Xvn2HoPgVtWw3v7d7J3rZrStpoHhP1A4Eea2Sqw7xdxClkSAog836Epx3QI3+PY8uyPOU55eMG1Dys9xFkifEA1Lc5/TbhTzSXTQINIOJT1cVI+nNeLlNcdB2luZsbIEL1PkKa7zO6rYqGcTvYOkL2d9H5Os94+wiHCCxmtP0a4jZ71jNU/4mHhpObEhj0cGDX0+GAVtxqp+DXCFF8QTSeiVHHZLg3xmK79VvJKgnCQOMpkYYBzWkhP10xu+LqHBX0m1xOv4ndWUeF5jxNn3tTd70XaAq8wDh0MGgyaDUhQEEUEYZiwUECGPBoxNLJyPyOrBhuTezJ1JGq7dGJEsUF7Ntw9t1Gk3Tz+KCJxlEO1CJL8Qf4qr8lP5Xn5y1yw2Fb3lK2bmrry4DvF5Zm5Gh7X08jjc01efJXUdpNXR5aseXq8muwaP+xXlzHmgjWPxHOw+/EtX5XMlymMFMXjVfPqS4R1WjE3359sfzs94i7PLrXWc62JizdWm5dn/WpI++6qvJPmVflPXvXx/GfNxGPiKTEmdornIYmXxS7xkthLqwviYG3HCJ2VhinSbZH6JNVgYJq89S9dP1t4vUZ/DPVRlBnM0lSJ93/CKmQ0nbkOb/qP28f8F+T3iuefKAIvbODImbptU3HvEKFlpW5zrgIXv9F98LZua6N+OPwEWDyrFq1SNZ8gvAEcdod6HugpmNOWls05Uocsn5O66cpiUsxQ20NSUtcl12VLFrOZVWLpdtiZ0x1uHKE5QvfEp0plk/qv8RGw/bBS+fmsUtl+ThrWgZf6b8C8/UXAeIuJAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAOklEQVQ4EWO8e/fuPwYGBkYgphlgAZmspKREMwtABjPR1HSo4aOWkBTKo8E1GlwkhQBJikdT1wgNLgAMSwQgckFvTgAAAABJRU5ErkJggg==);\n" +
                "}";

        String result = transformResource.transform(original);

        // No change
        assertEquals(original, result);
    }

    public void testReplaceRelativeWithFunkyCharacters()
    {
        String original =
                ".test {\n" +
                "    background-image: url(\'http://www.f\u00F8tex.dk\')\n" +
                "}\n" +
                ".sample {\n" +
                "    background-image: url(\"/f\u00F8tex/absolute.png\")\n" +
                "}\n" +
                ".works {\n" +
                "    background-image: url(\"f\u00F8tex.png\")\n" +
                "}";

        String result = transformResource.transform(original);

        String expected =
                ".test {\n" +
                "    background-image: url(\'http://www.f\u00F8tex.dk\')\n" +
                "}\n" +
                ".sample {\n" +
                "    background-image: url(\"/f\u00F8tex/absolute.png\")\n" +
                "}\n" +
                ".works {\n" +
                "    background-image: url(\"" + TRANSFORM_PREFIX + "f\u00F8tex.png\")\n" +
                "}";

        assertEquals(expected, result);
    }

    public void testReplaceRelativeWithNumbers()
    {
        String original =
                ".test {\n" +
                "    background-image: url(\'http://192.168.1.1:8080/test/absolute.png\')\n" +
                "}\n" +
                ".sample {\n" +
                "    background-image: url(\"/1/absolute.png\")\n" +
                "}\n" +
                ".works {\n" +
                "    background-image: url(\"../relative1.png\")\n" +
                "}";

        String result = transformResource.transform(original);

        String expected =
                ".test {\n" +
                "    background-image: url(\'http://192.168.1.1:8080/test/absolute.png\')\n" +
                "}\n" +
                ".sample {\n" +
                "    background-image: url(\"/1/absolute.png\")\n" +
                "}\n" +
                ".works {\n" +
                "    background-image: url(\"" + TRANSFORM_PREFIX + "../relative1.png\")\n" +
                "}";

        assertEquals(expected, result);
    }

    public void testReplaceRelativeWithSpacesBeforeParantheses()
    {
        String original =
                ".test {\n" +
                "    background-image: url (\"http://www.atlassian.com\")\n" +
                "}\n" +
                ".sample {\n" +
                "    background-image: url (\"/images/absolute.png\")\n" +
                "}\n" +
                ".works {\n" +
                "    background-image: url (\"relative.png\")\n" +
                "}";

        String result = transformResource.transform(original);

        String expected =
                ".test {\n" +
                "    background-image: url (\"http://www.atlassian.com\")\n" +
                "}\n" +
                ".sample {\n" +
                "    background-image: url (\"/images/absolute.png\")\n" +
                "}\n" +
                ".works {\n" +
                "    background-image: url(\"" + TRANSFORM_PREFIX + "relative.png\")\n" +
                "}";

        assertEquals(expected, result);
    }

    public void testReplaceRelativeWithSpacesAfterParantheses()
    {
        String original =
                ".test {\n" +
                "    background-image: url( \"http://www.atlassian.com\")\n" +
                "}\n" +
                ".sample {\n" +
                "    background-image: url( \"/images/absolute.png\")\n" +
                "}\n" +
                ".works {\n" +
                "    background-image: url( \"relative.png\")\n" +
                "}";

        String result = transformResource.transform(original);

        String expected =
                ".test {\n" +
                "    background-image: url( \"http://www.atlassian.com\")\n" +
                "}\n" +
                ".sample {\n" +
                "    background-image: url( \"/images/absolute.png\")\n" +
                "}\n" +
                ".works {\n" +
                "    background-image: url(\"" + TRANSFORM_PREFIX + "relative.png\")\n" +
                "}";

        assertEquals(expected, result);
    }
}
