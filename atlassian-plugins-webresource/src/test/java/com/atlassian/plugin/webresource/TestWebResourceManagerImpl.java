package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.servlet.AbstractFileServerServlet;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;

import java.util.*;
import java.io.StringWriter;

import org.dom4j.DocumentException;

public class TestWebResourceManagerImpl extends TestCase
{
    private Mock mockWebResourceIntegration;
    private Mock mockPluginAccessor;
    private WebResourceManager webResourceManager;

    private static final String ANIMAL_PLUGIN_VERSION = "2";
    private static final String BASEURL = "http://www.foo.com";
    private static final String SYSTEM_COUNTER = "123";
    private static final String SYSTEM_BUILD_NUMBER = "650";

    protected void setUp() throws Exception
    {
        super.setUp();

        mockPluginAccessor = new Mock(PluginAccessor.class);

        mockWebResourceIntegration = new Mock(WebResourceIntegration.class);
        mockWebResourceIntegration.matchAndReturn("getPluginAccessor", mockPluginAccessor.proxy());

        PluginResourceLocator pluginResourceLocator = new PluginResourceLocatorImpl((PluginAccessor) mockPluginAccessor.proxy(), null);
        webResourceManager = new WebResourceManagerImpl(pluginResourceLocator, (WebResourceIntegration) mockWebResourceIntegration.proxy());

        mockWebResourceIntegration.matchAndReturn("getBaseUrl", BASEURL);
        mockWebResourceIntegration.matchAndReturn("getBaseUrl", C.args(C.eq(WebResourceManager.UrlMode.ABSOLUTE)),
                                                  BASEURL);
        mockWebResourceIntegration.matchAndReturn("getBaseUrl", C.args(C.eq(WebResourceManager.UrlMode.RELATIVE)), "");
        mockWebResourceIntegration.matchAndReturn("getBaseUrl", C.args(C.eq(WebResourceManager.UrlMode.AUTO)), "");
        mockWebResourceIntegration.matchAndReturn("getSystemBuildNumber", SYSTEM_BUILD_NUMBER);
        mockWebResourceIntegration.matchAndReturn("getSystemCounter", SYSTEM_COUNTER);
    }

    protected void tearDown() throws Exception
    {
        webResourceManager = null;
        mockPluginAccessor = null;
        mockWebResourceIntegration = null;

        super.tearDown();
    }

    public void testRequireResources()
    {
        String resource1 = "test.atlassian:cool-stuff";
        String resource2 = "test.atlassian:hot-stuff";

        Mock mockPlugin = new Mock(Plugin.class);
        Plugin p = (Plugin) mockPlugin.proxy();
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resource1)),
            TestUtils.createWebResourceModuleDescriptor(resource1, p));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resource2)),
            TestUtils.createWebResourceModuleDescriptor(resource2, p));

        Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);
        webResourceManager.requireResource(resource1);
        webResourceManager.requireResource(resource2);
        webResourceManager.requireResource(resource1); // require again to test it only gets included once

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(2, resources.size());
        assertTrue(resources.contains(resource1));
        assertTrue(resources.contains(resource2));
    }

    public void testRequireResourcesWithDependencies()
    {
        String resource = "test.atlassian:cool-stuff";
        String dependencyResource = "test.atlassian:hot-stuff";

        Mock mockPlugin = new Mock(Plugin.class);
        Plugin p = (Plugin) mockPlugin.proxy();

        // cool-stuff depends on hot-stuff
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resource)),
            TestUtils.createWebResourceModuleDescriptor(resource, p, Collections.EMPTY_LIST, Collections.singletonList(dependencyResource)));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(dependencyResource)),
            TestUtils.createWebResourceModuleDescriptor(dependencyResource, p));

        Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);
        webResourceManager.requireResource(resource);

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(2, resources.size());
        Object[] resourceArray = resources.toArray();
        assertEquals(dependencyResource, resourceArray[0]);
        assertEquals(resource, resourceArray[1]);
    }

    public void testRequireResourcesWithCyclicDependency()
    {
        String resource1 = "test.atlassian:cool-stuff";
        String resource2 = "test.atlassian:hot-stuff";

        Mock mockPlugin = new Mock(Plugin.class);
        Plugin p = (Plugin) mockPlugin.proxy();

        // cool-stuff and hot-stuff depend on each other
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resource1)),
            TestUtils.createWebResourceModuleDescriptor(resource1, p, Collections.EMPTY_LIST, Collections.singletonList(resource2)));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resource2)),
            TestUtils.createWebResourceModuleDescriptor(resource2, p, Collections.EMPTY_LIST, Collections.singletonList(resource1)));

        Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);
        webResourceManager.requireResource(resource1);

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(2, resources.size());
        Object[] resourceArray = resources.toArray();
        assertEquals(resource2, resourceArray[0]);
        assertEquals(resource1, resourceArray[1]);
    }

    public void testRequireResourcesWithComplexCyclicDependency()
    {
        String resourceA = "test.atlassian:a";
        String resourceB = "test.atlassian:b";
        String resourceC = "test.atlassian:c";
        String resourceD = "test.atlassian:d";
        String resourceE = "test.atlassian:e";

        Mock mockPlugin = new Mock(Plugin.class);
        Plugin p = (Plugin) mockPlugin.proxy();

        // A depends on B, C
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceA)),
            TestUtils.createWebResourceModuleDescriptor(resourceA, p, Collections.EMPTY_LIST, Arrays.asList(resourceB, resourceC)));
        // B depends on D
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceB)),
            TestUtils.createWebResourceModuleDescriptor(resourceB, p, Collections.EMPTY_LIST, Collections.singletonList(resourceD)));
        // C has no dependencies
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceC)),
            TestUtils.createWebResourceModuleDescriptor(resourceC, p));
        // D depends on E, A (cyclic dependency)
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceD)),
            TestUtils.createWebResourceModuleDescriptor(resourceD, p, Collections.EMPTY_LIST, Arrays.asList(resourceE, resourceA)));
        // E has no dependencies
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceE)),
            TestUtils.createWebResourceModuleDescriptor(resourceE, p));

        Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);
        webResourceManager.requireResource(resourceA);
        // requiring a resource already included by A's dependencies shouldn't change the order
        webResourceManager.requireResource(resourceD);

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(5, resources.size());
        Object[] resourceArray = resources.toArray();
        assertEquals(resourceE, resourceArray[0]);
        assertEquals(resourceD, resourceArray[1]);
        assertEquals(resourceB, resourceArray[2]);
        assertEquals(resourceC, resourceArray[3]);
        assertEquals(resourceA, resourceArray[4]);
    }

    public void testRequireResourceWithDuplicateDependencies()
    {
        String resourceA = "test.atlassian:a";
        String resourceB = "test.atlassian:b";
        String resourceC = "test.atlassian:c";
        String resourceD = "test.atlassian:d";
        String resourceE = "test.atlassian:e";

        Mock mockPlugin = new Mock(Plugin.class);
        Plugin p = (Plugin) mockPlugin.proxy();

        // A depends on B, C
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceA)),
            TestUtils.createWebResourceModuleDescriptor(resourceA, p, Collections.EMPTY_LIST, Arrays.asList(resourceB, resourceC)));
        // B depends on D
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceB)),
            TestUtils.createWebResourceModuleDescriptor(resourceB, p, Collections.EMPTY_LIST, Collections.singletonList(resourceD)));
        // C depends on E
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceC)),
            TestUtils.createWebResourceModuleDescriptor(resourceC, p, Collections.EMPTY_LIST, Collections.singletonList(resourceE)));
        // D depends on C
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceD)),
            TestUtils.createWebResourceModuleDescriptor(resourceD, p, Collections.EMPTY_LIST, Collections.singletonList(resourceC)));
        // E has no dependencies
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceE)),
            TestUtils.createWebResourceModuleDescriptor(resourceE, p));

        Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);
        webResourceManager.requireResource(resourceA);

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(5, resources.size());
        Object[] resourceArray = resources.toArray();
        assertEquals(resourceE, resourceArray[0]);
        assertEquals(resourceC, resourceArray[1]);
        assertEquals(resourceD, resourceArray[2]);
        assertEquals(resourceB, resourceArray[3]);
        assertEquals(resourceA, resourceArray[4]);
    }

    public void testRequireSingleResourceGetsDeps() throws Exception
    {
        String resourceA = "test.atlassian:a";
        String resourceB = "test.atlassian:b";

        final String pluginVersion = "1";
        final Mock mockPlugin = new Mock(Plugin.class);
        PluginInformation pluginInfo = new PluginInformation();
        pluginInfo.setVersion(pluginVersion);
        mockPlugin.matchAndReturn("getPluginInformation", pluginInfo);

        Plugin p = (Plugin) mockPlugin.proxy();

        final List<ResourceDescriptor> resourceDescriptorsA = TestUtils.createResourceDescriptors("resourceA.css");
        final List<ResourceDescriptor> resourceDescriptorsB = TestUtils.createResourceDescriptors("resourceB.css", "resourceB-more.css");

        // A depends on B
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceA)),
            TestUtils.createWebResourceModuleDescriptor(resourceA, p, resourceDescriptorsA, Collections.singletonList(resourceB)));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceB)),
            TestUtils.createWebResourceModuleDescriptor(resourceB, p, resourceDescriptorsB, Collections.EMPTY_LIST));

        String s = webResourceManager.getResourceTags(resourceA);
        int indexA = s.indexOf(resourceA);
        int indexB = s.indexOf(resourceB);

        assertNotSame(-1, indexA);
        assertNotSame(-1, indexB);
        assertTrue(indexB < indexA);
    }
    
    public void testRequireResourcesAreClearedAfterIncludesResourcesIsCalled() throws Exception
    {
        final String moduleKey = "cool-resources";
        final String completeModuleKey = "test.atlassian:" + moduleKey;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.css", "more-cool.css", "cool.js");

        final String pluginVersion = "1";
        final Mock mockPlugin = new Mock(Plugin.class);
        PluginInformation pluginInfo = new PluginInformation();
        pluginInfo.setVersion(pluginVersion);
        mockPlugin.matchAndReturn("getPluginInformation", pluginInfo);

        Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey)),
            TestUtils.createWebResourceModuleDescriptor(completeModuleKey, (Plugin) mockPlugin.proxy(), resourceDescriptors1));

        // test requireResource() methods
        webResourceManager.requireResource(completeModuleKey);
        webResourceManager.includeResources(new StringWriter());
        
        StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.includeResources(requiredResourceWriter);
        assertEquals("", requiredResourceWriter.toString());
    }

    // testRequireResourceAndResourceTagMethods

    public void testRequireResourceAndResourceTagMethods() throws Exception
    {
        final String completeModuleKey = "test.atlassian:cool-resources";
        final String staticBase = setupRequireResourceAndResourceTagMethods(false, completeModuleKey);

        // test requireResource() methods
        StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey);
        String requiredResourceResult = webResourceManager.getRequiredResources();
        webResourceManager.includeResources(requiredResourceWriter);
        assertEquals(requiredResourceResult, requiredResourceWriter.toString());

        assertTrue(requiredResourceResult.contains("href=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".css"));
        assertTrue(requiredResourceResult.contains("src=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".js"));

        // test resourceTag() methods
        StringWriter resourceTagsWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey, resourceTagsWriter);
        String resourceTagsResult = webResourceManager.getResourceTags(completeModuleKey);
        assertEquals(resourceTagsResult, resourceTagsWriter.toString());

        // calling requireResource() or resourceTag() on a single webresource should be the same
        assertEquals(requiredResourceResult, resourceTagsResult);
    }

    public void testRequireResourceAndResourceTagMethodsWithAbsoluteUrlMode() throws Exception
    {
        testRequireResourceAndResourceTagMethods(WebResourceManager.UrlMode.ABSOLUTE, true);
    }

    public void testRequireResourceAndResourceTagMethodsWithRelativeUrlMode() throws Exception
    {
        testRequireResourceAndResourceTagMethods(WebResourceManager.UrlMode.RELATIVE, false);
    }

    public void testRequireResourceAndResourceTagMethodsWithAutoUrlMode() throws Exception
    {
        testRequireResourceAndResourceTagMethods(WebResourceManager.UrlMode.AUTO, false);
    }

    private void testRequireResourceAndResourceTagMethods(WebResourceManager.UrlMode urlMode, boolean baseUrlExpected)
        throws Exception
    {
        final String completeModuleKey = "test.atlassian:cool-resources";
        final String staticBase = setupRequireResourceAndResourceTagMethods(baseUrlExpected, completeModuleKey);

        // test requireResource() methods
        final StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey);
        final String requiredResourceResult = webResourceManager.getRequiredResources(urlMode);
        webResourceManager.includeResources(requiredResourceWriter, urlMode);
        assertEquals(requiredResourceResult, requiredResourceWriter.toString());

        assertTrue(requiredResourceResult.contains("href=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".css"));
        assertTrue(requiredResourceResult.contains("src=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".js"));

        // test resourceTag() methods
        StringWriter resourceTagsWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey, resourceTagsWriter, urlMode);
        String resourceTagsResult = webResourceManager.getResourceTags(completeModuleKey, urlMode);
        assertEquals(resourceTagsResult, resourceTagsWriter.toString());

        // calling requireResource() or resourceTag() on a single webresource should be the same
        assertEquals(requiredResourceResult, resourceTagsResult);
    }

    private String setupRequireResourceAndResourceTagMethods(boolean baseUrlExpected, String completeModuleKey)
        throws DocumentException
    {
        final List<ResourceDescriptor> resourceDescriptors1 =
            TestUtils.createResourceDescriptors("cool.css", "more-cool.css", "cool.js");

        final String pluginVersion = "1";
        final Mock mockPlugin = new Mock(Plugin.class);
        final PluginInformation pluginInfo = new PluginInformation();
        pluginInfo.setVersion(pluginVersion);
        mockPlugin.matchAndReturn("getPluginInformation", pluginInfo);

        final Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey)),
            TestUtils.createWebResourceModuleDescriptor(completeModuleKey, (Plugin) mockPlugin.proxy(), resourceDescriptors1));

        final String staticBase = (baseUrlExpected ? BASEURL : "") +
            "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX  + "/" + SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER +
            "/" + pluginVersion + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX + BatchPluginResource.URL_PREFIX;
        return staticBase;
    }

    // testRequireResourceWithCacheParameter

    public void testRequireResourceWithCacheParameter() throws Exception
    {
        final String completeModuleKey = "test.atlassian:no-cache-resources";

        final String expectedResult = setupRequireResourceWithCacheParameter(false, completeModuleKey);
        assertTrue(webResourceManager.getResourceTags(completeModuleKey).contains(expectedResult));
    }

    public void testRequireResourceWithCacheParameterAndAbsoluteUrlMode() throws Exception
    {
        testRequireResourceWithCacheParameter(WebResourceManager.UrlMode.ABSOLUTE, true);
    }

    public void testRequireResourceWithCacheParameterAndRelativeUrlMode() throws Exception
    {
        testRequireResourceWithCacheParameter(WebResourceManager.UrlMode.RELATIVE, false);
    }

    public void testRequireResourceWithCacheParameterAndAutoUrlMode() throws Exception
    {
        testRequireResourceWithCacheParameter(WebResourceManager.UrlMode.AUTO, false);
    }

    private void testRequireResourceWithCacheParameter(WebResourceManager.UrlMode urlMode, boolean baseUrlExpected)
        throws Exception
    {
        final String completeModuleKey = "test.atlassian:no-cache-resources";
        final String expectedResult = setupRequireResourceWithCacheParameter(baseUrlExpected, completeModuleKey);
        assertTrue(webResourceManager.getResourceTags(completeModuleKey, urlMode).contains(expectedResult));
    }

    private String setupRequireResourceWithCacheParameter(boolean baseUrlExpected, String completeModuleKey)
        throws DocumentException
    {
        final Mock mockPlugin = new Mock(Plugin.class);

        final Map<String, String> params = new HashMap<String, String>();
        params.put("cache", "false");
        ResourceDescriptor resourceDescriptor = TestUtils.createResourceDescriptor("no-cache.js", params);

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey)),
                TestUtils.createWebResourceModuleDescriptor(completeModuleKey, (Plugin) mockPlugin.proxy(),
                Collections.singletonList(resourceDescriptor)));

        return "src=\"" + (baseUrlExpected ? BASEURL : "") + BatchPluginResource.URL_PREFIX +
               "/" + completeModuleKey + "/" + completeModuleKey + ".js?cache=false";
    }

    // testGetStaticResourcePrefix

    public void testGetStaticResourcePrefix()
    {
        final String expectedPrefix = setupGetStaticResourcePrefix(false);
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix());
    }

    public void testGetStaticResourcePrefixWithAbsoluteUrlMode()
    {
        testGetStaticResourcePrefix(WebResourceManager.UrlMode.ABSOLUTE, true);
    }

    public void testGetStaticResourcePrefixWithRelativeUrlMode()
    {
        testGetStaticResourcePrefix(WebResourceManager.UrlMode.RELATIVE, false);
    }

    public void testGetStaticResourcePrefixWithAutoUrlMode()
    {
        testGetStaticResourcePrefix(WebResourceManager.UrlMode.AUTO, false);
    }

    private void testGetStaticResourcePrefix(WebResourceManager.UrlMode urlMode, boolean baseUrlExpected)
    {
        final String expectedPrefix = setupGetStaticResourcePrefix(baseUrlExpected);
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix(urlMode));
    }

    private String setupGetStaticResourcePrefix(boolean baseUrlExpected)
    {
        return (baseUrlExpected ? BASEURL : "") +
            "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" +
            SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
    }

    // testGetStaticResourcePrefixWithCounter

    public void testGetStaticResourcePrefixWithCounter()
    {
        final String resourceCounter = "456";
        final String expectedPrefix = setupGetStaticResourcePrefixWithCounter(false, resourceCounter);
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix(resourceCounter));
    }

    public void testGetStaticResourcePrefixWithCounterAndAbsoluteUrlMode()
    {
        testGetStaticResourcePrefixWithCounter(WebResourceManager.UrlMode.ABSOLUTE, true);
    }

    public void testGetStaticResourcePrefixWithCounterAndRelativeUrlMode()
    {
        testGetStaticResourcePrefixWithCounter(WebResourceManager.UrlMode.RELATIVE, false);
    }

    public void testGetStaticResourcePrefixWithCounterAndAutoUrlMode()
    {
        testGetStaticResourcePrefixWithCounter(WebResourceManager.UrlMode.AUTO, false);
    }

    private void testGetStaticResourcePrefixWithCounter(WebResourceManager.UrlMode urlMode, boolean baseUrlExpected)
    {
        final String resourceCounter = "456";
        final String expectedPrefix = setupGetStaticResourcePrefixWithCounter(baseUrlExpected, resourceCounter);
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix(resourceCounter, urlMode));
    }

    private String setupGetStaticResourcePrefixWithCounter(boolean baseUrlExpected, String resourceCounter)
    {
        return (baseUrlExpected ? BASEURL : "") +
            "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" +
            SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + resourceCounter +
            "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
    }

    // testGetStaticPluginResourcePrefix

    public void testGetStaticPluginResourcePrefix()
    {
        final String moduleKey = "confluence.extra.animal:animal";
        final String resourceName = "foo.js";

        final String expectedPrefix = setupGetStaticPluginResourcePrefix(false, moduleKey, resourceName);        

        assertEquals(expectedPrefix, webResourceManager.getStaticPluginResource(moduleKey, resourceName));
    }

    public void testGetStaticPluginResourcePrefixWithAbsoluteUrlMode()
    {
        testGetStaticPluginResourcePrefix(WebResourceManager.UrlMode.ABSOLUTE, true);
    }

    public void testGetStaticPluginResourcePrefixWithRelativeUrlMode()
    {
        testGetStaticPluginResourcePrefix(WebResourceManager.UrlMode.RELATIVE, false);
    }

    public void testGetStaticPluginResourcePrefixWithAutoUrlMode()
    {
        testGetStaticPluginResourcePrefix(WebResourceManager.UrlMode.AUTO, false);
    }

    private void testGetStaticPluginResourcePrefix(WebResourceManager.UrlMode urlMode, boolean baseUrlExpected)
    {
        final String moduleKey = "confluence.extra.animal:animal";
        final String resourceName = "foo.js";

        final String expectedPrefix = setupGetStaticPluginResourcePrefix(baseUrlExpected, moduleKey, resourceName);

        assertEquals(expectedPrefix, webResourceManager.getStaticPluginResource(moduleKey, resourceName, urlMode));
    }

    private String setupGetStaticPluginResourcePrefix(boolean baseUrlExpected, String moduleKey, String resourceName)
    {
        final Plugin animalPlugin = new StaticPlugin();
        animalPlugin.setKey("confluence.extra.animal");
        animalPlugin.setPluginsVersion(Integer.parseInt(ANIMAL_PLUGIN_VERSION));

        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(moduleKey)),
            TestUtils.createWebResourceModuleDescriptor(moduleKey, animalPlugin));

        return (baseUrlExpected ? BASEURL : "") +
            "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER +
            "/" + SYSTEM_COUNTER + "/" + ANIMAL_PLUGIN_VERSION + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX +
            "/" + AbstractFileServerServlet.SERVLET_PATH + "/" + AbstractFileServerServlet.RESOURCE_URL_PREFIX +
            "/" + moduleKey + "/" + resourceName;
    }

}
