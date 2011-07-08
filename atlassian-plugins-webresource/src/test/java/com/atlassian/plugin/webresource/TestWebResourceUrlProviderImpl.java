package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.servlet.AbstractFileServerServlet;
import junit.framework.TestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

public class TestWebResourceUrlProviderImpl extends TestCase
{
    private static final String ANIMAL_PLUGIN_VERSION = "2";
    private static final String BASEURL = "http://www.foo.com";
    private static final String SYSTEM_COUNTER = "123";
    private static final String SYSTEM_BUILD_NUMBER = "650";

    @Mock
    private WebResourceIntegration mockWebResourceIntegration;
    @Mock
    private PluginAccessor mockPluginAccessor;

    private Plugin testPlugin;
    private WebResourceUrlProvider urlProvider;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        MockitoAnnotations.initMocks(this);
        urlProvider = new WebResourceUrlProviderImpl(mockWebResourceIntegration);

        testPlugin = TestUtils.createTestPlugin();
        when(mockWebResourceIntegration.getPluginAccessor()).thenReturn(mockPluginAccessor);
        when(mockWebResourceIntegration.getBaseUrl()).thenReturn(BASEURL);
        when(mockWebResourceIntegration.getBaseUrl(UrlMode.ABSOLUTE)).thenReturn(BASEURL);
        when(mockWebResourceIntegration.getBaseUrl(UrlMode.RELATIVE)).thenReturn("");
        when(mockWebResourceIntegration.getBaseUrl(UrlMode.AUTO)).thenReturn("");
        when(mockWebResourceIntegration.getSystemBuildNumber()).thenReturn(SYSTEM_BUILD_NUMBER);
        when(mockWebResourceIntegration.getSystemCounter()).thenReturn(SYSTEM_COUNTER);
        when(mockWebResourceIntegration.getStaticResourceLocale()).thenReturn(null);
    }

    @Override
    public void tearDown() throws Exception
    {
        mockWebResourceIntegration = null;
        mockPluginAccessor = null;

        urlProvider = null;
        testPlugin = null;
        super.tearDown();
    }

    // testGetStaticResourcePrefix

    public void testGetStaticResourcePrefix()
    {
        final String expectedPrefix = setupGetStaticResourcePrefix(false);
        assertEquals(expectedPrefix, urlProvider.getStaticResourcePrefix(UrlMode.AUTO));
    }

    public void testGetStaticResourcePrefixWithAbsoluteUrlMode()
    {
        testGetStaticResourcePrefix(UrlMode.ABSOLUTE, true);
    }

    public void testGetStaticResourcePrefixWithRelativeUrlMode()
    {
        testGetStaticResourcePrefix(UrlMode.RELATIVE, false);
    }

    public void testGetStaticResourcePrefixWithAutoUrlMode()
    {
        testGetStaticResourcePrefix(UrlMode.AUTO, false);
    }

    private void testGetStaticResourcePrefix(UrlMode urlMode, boolean baseUrlExpected)
    {
        final String expectedPrefix = setupGetStaticResourcePrefix(baseUrlExpected);
        assertEquals(expectedPrefix, urlProvider.getStaticResourcePrefix(urlMode));
    }

    private String setupGetStaticResourcePrefix(boolean baseUrlExpected)
    {
        return (baseUrlExpected ? BASEURL : "") +
            "/" + WebResourceUrlProviderImpl.STATIC_RESOURCE_PREFIX + "/" +
            SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + WebResourceUrlProviderImpl.STATIC_RESOURCE_SUFFIX;
    }

    // testGetStaticResourcePrefixWithCounter

    public void testGetStaticResourcePrefixWithCounter()
    {
        final String resourceCounter = "456";
        final String expectedPrefix = setupGetStaticResourcePrefixWithCounter(false, resourceCounter);
        assertEquals(expectedPrefix, urlProvider.getStaticResourcePrefix(resourceCounter, UrlMode.AUTO));
    }

    public void testGetStaticResourcePrefixWithCounterAndAbsoluteUrlMode()
    {
        testGetStaticResourcePrefixWithCounter(UrlMode.ABSOLUTE, true);
    }

    public void testGetStaticResourcePrefixWithCounterAndRelativeUrlMode()
    {
        testGetStaticResourcePrefixWithCounter(UrlMode.RELATIVE, false);
    }

    public void testGetStaticResourcePrefixWithCounterAndAutoUrlMode()
    {
        testGetStaticResourcePrefixWithCounter(UrlMode.AUTO, false);
    }

    private void testGetStaticResourcePrefixWithCounter(UrlMode urlMode, boolean baseUrlExpected)
    {
        final String resourceCounter = "456";
        final String expectedPrefix = setupGetStaticResourcePrefixWithCounter(baseUrlExpected, resourceCounter);
        assertEquals(expectedPrefix, urlProvider.getStaticResourcePrefix(resourceCounter, urlMode));
    }

    private String setupGetStaticResourcePrefixWithCounter(boolean baseUrlExpected, String resourceCounter)
    {
        return (baseUrlExpected ? BASEURL : "") +
            "/" + WebResourceUrlProviderImpl.STATIC_RESOURCE_PREFIX + "/" +
            SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + resourceCounter +
            "/" + WebResourceUrlProviderImpl.STATIC_RESOURCE_SUFFIX;
    }

    // testGetStaticPluginResourcePrefix

    public void testGetStaticPluginResourcePrefix()
    {
        final String moduleKey = "confluence.extra.animal:animal";
        final String resourceName = "foo.js";

        final String expectedPrefix = setupGetStaticPluginResourcePrefix(false, moduleKey, resourceName);        

        assertEquals(expectedPrefix, urlProvider.getStaticPluginResourceUrl(moduleKey, resourceName, UrlMode.AUTO));
    }

    public void testGetStaticPluginResourcePrefixWithAbsoluteUrlMode()
    {
        testGetStaticPluginResourcePrefix(UrlMode.ABSOLUTE, true);
    }

    public void testGetStaticPluginResourcePrefixWithRelativeUrlMode()
    {
        testGetStaticPluginResourcePrefix(UrlMode.RELATIVE, false);
    }

    public void testGetStaticPluginResourcePrefixWithAutoUrlMode()
    {
        testGetStaticPluginResourcePrefix(UrlMode.AUTO, false);
    }

    private void testGetStaticPluginResourcePrefix(UrlMode urlMode, boolean baseUrlExpected)
    {
        final String moduleKey = "confluence.extra.animal:animal";
        final String resourceName = "foo.js";

        final String expectedPrefix = setupGetStaticPluginResourcePrefix(baseUrlExpected, moduleKey, resourceName);

        assertEquals(expectedPrefix, urlProvider.getStaticPluginResourceUrl(moduleKey, resourceName, urlMode));
    }

    private String setupGetStaticPluginResourcePrefix(boolean baseUrlExpected, String moduleKey, String resourceName)
    {
        final Plugin animalPlugin = new StaticPlugin();
        animalPlugin.setKey("confluence.extra.animal");
        animalPlugin.setPluginsVersion(5);
        animalPlugin.getPluginInformation().setVersion(ANIMAL_PLUGIN_VERSION);

        mockEnabledPluginModule(moduleKey, TestUtils.createWebResourceModuleDescriptor(moduleKey, animalPlugin));

        return (baseUrlExpected ? BASEURL : "") +
            "/" + WebResourceUrlProviderImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER +
            "/" + SYSTEM_COUNTER + "/" + ANIMAL_PLUGIN_VERSION + "/" + WebResourceUrlProviderImpl.STATIC_RESOURCE_SUFFIX +
            "/" + AbstractFileServerServlet.SERVLET_PATH + "/" + AbstractFileServerServlet.RESOURCE_URL_PREFIX +
            "/" + moduleKey + "/" + resourceName;
    }


    public void testGetStaticPrefixResourceWithLocale()
    {
        testGetStaticPrefixResourceWithLocale(UrlMode.ABSOLUTE, true);
        testGetStaticPrefixResourceWithLocale(UrlMode.RELATIVE, false);
        testGetStaticPrefixResourceWithLocale(UrlMode.AUTO, false);
    }

    private void testGetStaticPrefixResourceWithLocale(UrlMode urlMode, boolean baseUrlExpected)
    {
        String expected = setupGetStaticPrefixResourceWithLocale(baseUrlExpected);
        assertEquals(expected, urlProvider.getStaticResourcePrefix(urlMode));
    }

    private String setupGetStaticPrefixResourceWithLocale(boolean baseUrlExpected)
    {
        when(mockWebResourceIntegration.getStaticResourceLocale()).thenReturn("de_DE");
        return (baseUrlExpected ? BASEURL : "") +
                "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" +
                "de_DE/" +
                SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER +
                "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
    }

    public void testStaticPluginResourceWithLocale()
    {
        testStaticPluginResourceWithLocale(UrlMode.ABSOLUTE, true);
        testStaticPluginResourceWithLocale(UrlMode.RELATIVE, false);
        testStaticPluginResourceWithLocale(UrlMode.AUTO, false);
    }

    private void testStaticPluginResourceWithLocale(UrlMode urlMode, boolean baseUrlExpected)
    {
        final String moduleKey = "confluence.extra.animal:animal";
        final String resourceName = "foo.js";
        String expected = setupStaticPluginResourceWithLocale(moduleKey, resourceName, baseUrlExpected);
        assertEquals(expected, urlProvider.getStaticPluginResourceUrl(moduleKey, resourceName, urlMode));
    }

    private String setupStaticPluginResourceWithLocale(final String moduleKey, final String resourceName, boolean baseUrlExpected)
    {
        when(mockWebResourceIntegration.getStaticResourceLocale()).thenReturn("de_DE");
        String ver = "2";
        final Plugin animalPlugin = new StaticPlugin();
        animalPlugin.setKey("confluence.extra.animal");
        animalPlugin.setPluginsVersion(5);
        animalPlugin.getPluginInformation().setVersion(ver);

        final ModuleDescriptor moduleDescriptor = TestUtils.createWebResourceModuleDescriptor(moduleKey, animalPlugin);
        when(mockPluginAccessor.getEnabledPluginModule(moduleKey)).thenReturn(moduleDescriptor);

        return (baseUrlExpected ? BASEURL : "") +
                "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/de_DE/" + SYSTEM_BUILD_NUMBER +
                "/" + SYSTEM_COUNTER + "/" + ver + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX +
                "/" + AbstractFileServerServlet.SERVLET_PATH + "/" + AbstractFileServerServlet.RESOURCE_URL_PREFIX +
                "/" + moduleKey + "/" + resourceName;
    }

    private void mockEnabledPluginModule(final String key, final ModuleDescriptor md)
    {
        when(mockPluginAccessor.getEnabledPluginModule(key)).thenReturn(md);
    }
}
