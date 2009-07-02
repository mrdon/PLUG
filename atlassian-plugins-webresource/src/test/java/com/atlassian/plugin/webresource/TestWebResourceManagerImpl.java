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

    public void testRequireResourceAndResourceTagMethods() throws Exception
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
        StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey);
        String requiredResourceResult = webResourceManager.getRequiredResources();
        webResourceManager.includeResources(requiredResourceWriter);
        assertEquals(requiredResourceResult, requiredResourceWriter.toString());

        String staticBase = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX  + "/" + SYSTEM_BUILD_NUMBER
            + "/" + SYSTEM_COUNTER + "/" + pluginVersion + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX + BatchPluginResource.URL_PREFIX;

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

    public void testRequireResourceWithCacheParameter() throws Exception
    {
        final String moduleKey = "no-cache-resources";
        final String completeModuleKey = "test.atlassian:" + moduleKey;

        final Mock mockPlugin = new Mock(Plugin.class);

        Map<String, String> params = new HashMap<String, String>();
        params.put("cache", "false");
        ResourceDescriptor resourceDescriptor = TestUtils.createResourceDescriptor("no-cache.js", params);

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey)),
                TestUtils.createWebResourceModuleDescriptor(completeModuleKey, (Plugin) mockPlugin.proxy(),
                Collections.singletonList(resourceDescriptor)));

        String resourceTagsResult = webResourceManager.getResourceTags(completeModuleKey);
        assertTrue(resourceTagsResult.contains("src=\"" + BASEURL + BatchPluginResource.URL_PREFIX + "/"
            + completeModuleKey + "/" + completeModuleKey + ".js?cache=false"));
    }

    public void testGetStaticResourcePrefix()
    {
        String expectedPrefix = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" +
            SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix());
    }

    public void testGetStaticResourcePrefixWithCounter()
    {
        String resourceCounter = "456";
        String expectedPrefix = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" +
            SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + resourceCounter + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix(resourceCounter));
    }

    public void testGetStaticPluginResourcePrefix()
    {
        final String moduleKey = "confluence.extra.animal:animal";

        final Plugin animalPlugin = new StaticPlugin();
        animalPlugin.setKey("confluence.extra.animal");
        animalPlugin.setPluginsVersion(Integer.parseInt(ANIMAL_PLUGIN_VERSION));

        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(moduleKey)),
            TestUtils.createWebResourceModuleDescriptor(moduleKey, animalPlugin));

        String resourceName = "foo.js";
        String expectedPrefix = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER +
            "/" + SYSTEM_COUNTER + "/" + ANIMAL_PLUGIN_VERSION + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX + "/" + AbstractFileServerServlet
            .SERVLET_PATH + "/" + AbstractFileServerServlet.RESOURCE_URL_PREFIX + "/" + moduleKey + "/" + resourceName;

        assertEquals(expectedPrefix, webResourceManager.getStaticPluginResource(moduleKey, resourceName));
    }

    public void testGetRequiredResourcesWithType() throws Exception
    {
        final String moduleKey = "cool-resources";
        final String completeModuleKey = "test.atlassian:" + moduleKey;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.css", "cool.js", "more-cool.css");

        final String pluginVersion = "1";
        final Mock mockPlugin = new Mock(Plugin.class);
        PluginInformation pluginInfo = new PluginInformation();
        pluginInfo.setVersion(pluginVersion);
        mockPlugin.matchAndReturn("getPluginInformation", pluginInfo);

        Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey)),
            TestUtils.createWebResourceModuleDescriptor(completeModuleKey, (Plugin) mockPlugin.proxy(), resourceDescriptors1));

        // test includeResources(writer, type) method
        webResourceManager.requireResource(completeModuleKey);

        String staticBase = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX  + "/" + SYSTEM_BUILD_NUMBER
            + "/" + SYSTEM_COUNTER + "/" + pluginVersion + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX + BatchPluginResource.URL_PREFIX;

        String cssRef = "href=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".css";
        String jsRef = "src=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".js";

        // CSS
        String requiredResourceResult = webResourceManager.getRequiredResources("css");
        assertTrue(requiredResourceResult.contains(cssRef));
        assertFalse(requiredResourceResult.contains(jsRef));

        // JS
        requiredResourceResult = webResourceManager.getRequiredResources("js");
        assertFalse(requiredResourceResult.contains(cssRef));
        assertTrue(requiredResourceResult.contains(jsRef));

        // BOTH
        requiredResourceResult = webResourceManager.getRequiredResources();
        assertTrue(requiredResourceResult.contains(cssRef));
        assertTrue(requiredResourceResult.contains(jsRef));
    }

    public void testGetRequiredResourcesOrdersByType() throws Exception
    {
        final String moduleKey1 = "cool-resources";
        final String moduleKey2 = "hot-resources";
        final String completeModuleKey1 = "test.atlassian:" + moduleKey1;
        final String completeModuleKey2 = "test.atlassian:" + moduleKey2;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.js", "cool.css", "more-cool.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("hot.js", "hot.css", "more-hot.css");

        final String pluginVersion = "1";
        final Mock mockPlugin = new Mock(Plugin.class);
        PluginInformation pluginInfo = new PluginInformation();
        pluginInfo.setVersion(pluginVersion);
        mockPlugin.matchAndReturn("getPluginInformation", pluginInfo);

        Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey1)),
            TestUtils.createWebResourceModuleDescriptor(completeModuleKey1, (Plugin) mockPlugin.proxy(), resourceDescriptors1));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey2)),
                    TestUtils.createWebResourceModuleDescriptor(completeModuleKey2, (Plugin) mockPlugin.proxy(), resourceDescriptors2));

        // test includeResources(writer, type) method
        webResourceManager.requireResource(completeModuleKey1);
        webResourceManager.requireResource(completeModuleKey2);

        String staticBase = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX  + "/" + SYSTEM_BUILD_NUMBER
            + "/" + SYSTEM_COUNTER + "/" + pluginVersion + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX + BatchPluginResource.URL_PREFIX;

        String cssRef1 = "href=\"" + staticBase + "/" + completeModuleKey1 + "/" + completeModuleKey1 + ".css";
        String cssRef2 = "href=\"" + staticBase + "/" + completeModuleKey2 + "/" + completeModuleKey2 + ".css";
        String jsRef1 = "src=\"" + staticBase + "/" + completeModuleKey1 + "/" + completeModuleKey1 + ".js";
        String jsRef2 = "src=\"" + staticBase + "/" + completeModuleKey2 + "/" + completeModuleKey2 + ".js";

        String requiredResourceResult = webResourceManager.getRequiredResources();

        assertTrue(requiredResourceResult.contains(cssRef1));
        assertTrue(requiredResourceResult.contains(cssRef2));
        assertTrue(requiredResourceResult.contains(jsRef1));
        assertTrue(requiredResourceResult.contains(jsRef2));

        int cssRef1Index = requiredResourceResult.indexOf(cssRef1);
        int cssRef2Index = requiredResourceResult.indexOf(cssRef2);
        int jsRef1Index = requiredResourceResult.indexOf(jsRef1);
        int jsRef2Index = requiredResourceResult.indexOf(jsRef2);

        assertTrue(cssRef1Index < jsRef1Index);
        assertTrue(cssRef2Index < jsRef2Index);
        assertTrue(cssRef2Index < jsRef1Index);
    }
}
