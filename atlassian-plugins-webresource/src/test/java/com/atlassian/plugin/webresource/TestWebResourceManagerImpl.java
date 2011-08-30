package com.atlassian.plugin.webresource;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceDescriptor;

import com.google.common.base.Supplier;
import org.dom4j.DocumentException;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class TestWebResourceManagerImpl extends TestCase
{

    private WebResourceIntegration mockWebResourceIntegration = mock(WebResourceIntegration.class);
    private PluginAccessor mockPluginAccessor = mock(PluginAccessor.class);
    private final WebResourceUrlProvider mockUrlProvider = mock(WebResourceUrlProvider.class);
    private ResourceBatchingConfiguration mockBatchingConfiguration = mock(ResourceBatchingConfiguration.class);
    private PluginResourceLocator pluginResourceLocator;
    private WebResourceManagerImpl webResourceManager;
    private Plugin testPlugin;

    private static final String BASEURL = "http://www.foo.com";
    private static final String SYSTEM_COUNTER = "123";
    private static final String SYSTEM_BUILD_NUMBER = "650";

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        when(mockWebResourceIntegration.getPluginAccessor()).thenReturn(mockPluginAccessor);
        when(mockWebResourceIntegration.getSuperBatchVersion()).thenReturn(SYSTEM_BUILD_NUMBER);

        pluginResourceLocator = new PluginResourceLocatorImpl(mockWebResourceIntegration, null, mockUrlProvider, mockBatchingConfiguration);
        webResourceManager = new WebResourceManagerImpl(pluginResourceLocator, mockWebResourceIntegration, mockUrlProvider,
                mockBatchingConfiguration);

        when(mockUrlProvider.getBaseUrl()).thenReturn(BASEURL);
        when(mockUrlProvider.getBaseUrl(UrlMode.ABSOLUTE)).thenReturn(BASEURL);
        when(mockUrlProvider.getBaseUrl(UrlMode.AUTO)).thenReturn("");
        when(mockUrlProvider.getBaseUrl(UrlMode.RELATIVE)).thenReturn("");
        when(mockBatchingConfiguration.isPluginWebResourceBatchingEnabled()).thenReturn(true);
        testPlugin = TestUtils.createTestPlugin();
    }

    @Override
    protected void tearDown() throws Exception
    {
        webResourceManager = null;
        mockPluginAccessor = null;
        mockWebResourceIntegration = null;
        testPlugin = null;
        mockBatchingConfiguration = null;

        super.tearDown();
    }

    private void mockEnabledPluginModule(final String key)
    {
        final ModuleDescriptor md = TestUtils.createWebResourceModuleDescriptor(key, testPlugin);
        mockEnabledPluginModule(key, md);
    }

    private void mockEnabledPluginModule(final String key, final ModuleDescriptor md)
    {
        when(mockPluginAccessor.getEnabledPluginModule(key)).thenReturn(md);
    }

    public void testRequireResources()
    {
        final String resource1 = "test.atlassian:cool-stuff";
        final String resource2 = "test.atlassian:hot-stuff";

        mockEnabledPluginModule(resource1);
        mockEnabledPluginModule(resource2);

        final Map<String, Object> requestCache = setupRequestCache();
        webResourceManager.requireResource(resource1);
        webResourceManager.requireResource(resource2);
        webResourceManager.requireResource(resource1); // require again to test it only gets included once

        final Collection<?> resources = (Collection<?>) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(2, resources.size());
        assertTrue(resources.contains(resource1));
        assertTrue(resources.contains(resource2));
    }

    public void testRequireResourcesWithCondition() throws ClassNotFoundException
    {
        final String resource1 = "test.atlassian:cool-stuff";
        final String resource2 = "test.atlassian:hot-stuff";
        final Plugin plugin = TestUtils.createTestPlugin("test.atlassian", "1", AlwaysTrueCondition.class, AlwaysFalseCondition.class);

        mockEnabledPluginModule(resource1,
                new WebResourceModuleDescriptorBuilder(plugin, "cool-stuff").setCondition(AlwaysTrueCondition.class).addDescriptor("cool.js").build());
        mockEnabledPluginModule(resource2,
                new WebResourceModuleDescriptorBuilder(plugin, "hot-stuff").setCondition(AlwaysFalseCondition.class).addDescriptor("hot.js").build());

        setupRequestCache();
        webResourceManager.requireResource(resource1);
        webResourceManager.requireResource(resource2);

        final String tags = webResourceManager.getRequiredResources();
        assertTrue(tags.contains(resource1));
        assertFalse(tags.contains(resource2));
    }

    public void testRequireResourcesWithDependencies()
    {
        final String resource = "test.atlassian:cool-stuff";
        final String dependencyResource = "test.atlassian:hot-stuff";

        // cool-stuff depends on hot-stuff
        mockEnabledPluginModule(resource, TestUtils.createWebResourceModuleDescriptor(resource, testPlugin,
                Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(dependencyResource)));
        mockEnabledPluginModule(dependencyResource, TestUtils.createWebResourceModuleDescriptor(dependencyResource, testPlugin));

        final Map<String, Object> requestCache = setupRequestCache();
        webResourceManager.requireResource(resource);

        final Collection<?> resources = (Collection<?>) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(2, resources.size());
        final Object[] resourceArray = resources.toArray();
        assertEquals(dependencyResource, resourceArray[0]);
        assertEquals(resource, resourceArray[1]);
    }

    public void testRequireResourcesWithDependencyHiddenByCondition() throws ClassNotFoundException
    {
        final String resource1 = "test.atlassian:cool-stuff";
        final String resource2 = "test.atlassian:hot-stuff";
        final Plugin plugin = TestUtils.createTestPlugin("test.atlassian", "1", AlwaysTrueCondition.class, AlwaysFalseCondition.class);

        mockEnabledPluginModule(
                resource1,
                new WebResourceModuleDescriptorBuilder(plugin, "cool-stuff").setCondition(AlwaysTrueCondition.class).addDependency(resource2).addDescriptor(
                        "cool.js").build());
        mockEnabledPluginModule(resource2,
                new WebResourceModuleDescriptorBuilder(plugin, "hot-stuff").setCondition(AlwaysFalseCondition.class).addDescriptor("hot.js").build());

        setupRequestCache();
        webResourceManager.requireResource(resource1);

        final String tags = webResourceManager.getRequiredResources();
        assertTrue(tags.contains(resource1));
        assertFalse(tags.contains(resource2));
    }

    public void testRequireResourcesWithCyclicDependency()
    {
        final String resource1 = "test.atlassian:cool-stuff";
        final String resource2 = "test.atlassian:hot-stuff";

        // cool-stuff and hot-stuff depend on each other
        mockEnabledPluginModule(resource1, TestUtils.createWebResourceModuleDescriptor(resource1, testPlugin,
                Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(resource2)));
        mockEnabledPluginModule(resource2, TestUtils.createWebResourceModuleDescriptor(resource2, testPlugin,
                Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(resource1)));

        final Map<String, Object> requestCache = setupRequestCache();
        webResourceManager.requireResource(resource1);

        final Collection<?> resources = (Collection<?>) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(2, resources.size());
        final Object[] resourceArray = resources.toArray();
        assertEquals(resource2, resourceArray[0]);
        assertEquals(resource1, resourceArray[1]);
    }

    public void testRequireResourcesWithComplexCyclicDependency()
    {
        final String resourceA = "test.atlassian:a";
        final String resourceB = "test.atlassian:b";
        final String resourceC = "test.atlassian:c";
        final String resourceD = "test.atlassian:d";
        final String resourceE = "test.atlassian:e";

        // A depends on B, C
        mockEnabledPluginModule(resourceA, TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin,
                Collections.<ResourceDescriptor>emptyList(), Arrays.asList(resourceB, resourceC)));
        // B depends on D
        mockEnabledPluginModule(resourceB, TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin,
                Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(resourceD)));
        // C has no dependencies
        mockEnabledPluginModule(resourceC, TestUtils.createWebResourceModuleDescriptor(resourceC, testPlugin));
        // D depends on E, A (cyclic dependency)
        mockEnabledPluginModule(resourceD, TestUtils.createWebResourceModuleDescriptor(resourceD, testPlugin,
                Collections.<ResourceDescriptor>emptyList(), Arrays.asList(resourceE, resourceA)));
        // E has no dependencies
        mockEnabledPluginModule(resourceE, TestUtils.createWebResourceModuleDescriptor(resourceE, testPlugin));

        final Map<String, Object> requestCache = setupRequestCache();
        webResourceManager.requireResource(resourceA);
        // requiring a resource already included by A's dependencies shouldn't change the order
        webResourceManager.requireResource(resourceD);

        final Collection<?> resources = (Collection<?>) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(5, resources.size());
        final Object[] resourceArray = resources.toArray();
        assertEquals(resourceE, resourceArray[0]);
        assertEquals(resourceD, resourceArray[1]);
        assertEquals(resourceB, resourceArray[2]);
        assertEquals(resourceC, resourceArray[3]);
        assertEquals(resourceA, resourceArray[4]);
    }

    public void testGetResourceContext() throws Exception
    {
        when(mockBatchingConfiguration.isContextBatchingEnabled()).thenReturn(true);
        final String resourceA = "test.atlassian:a";
        final String resourceB = "test.atlassian:b";
        final String resourceC = "test.atlassian:c";

        final WebResourceModuleDescriptor descriptor1 = TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin,
                TestUtils.createResourceDescriptors("resourceA.css"), Collections.<String>emptyList(), Collections.<String>emptySet());
        final WebResourceModuleDescriptor descriptor2 = TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin,
                TestUtils.createResourceDescriptors("resourceB.css"), Collections.<String>emptyList(), new HashSet<String>()
        {
            {
                add("foo");
            }
        });
        final WebResourceModuleDescriptor descriptor3 = TestUtils.createWebResourceModuleDescriptor(resourceC, testPlugin,
                TestUtils.createResourceDescriptors("resourceC.css"), Collections.<String>emptyList(), new HashSet<String>()
        {
            {
                add("foo");
                add("bar");
            }
        });

        final List<WebResourceModuleDescriptor> descriptors = Arrays.asList(descriptor1, descriptor2, descriptor3);

        when(mockPluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class)).thenReturn(descriptors);
        mockEnabledPluginModule(resourceA, descriptor1);
        mockEnabledPluginModule(resourceB, descriptor2);
        mockEnabledPluginModule(resourceC, descriptor3);

        setupRequestCache();

        // write includes for all resources for "foo":
        webResourceManager.requireResourcesForContext("foo");
        StringWriter writer = new StringWriter();
        webResourceManager.includeResources(writer);
        String resources = writer.toString();
        assertFalse(resources.contains(resourceA + ".css"));
        assertFalse(resources.contains(resourceB + ".css"));
        assertFalse(resources.contains(resourceC + ".css"));
        assertTrue(resources.contains("/contextbatch/css/foo/batch.css"));
        assertFalse(resources.contains("/contextbatch/css/bar/batch.css"));

        // write includes for all resources for "bar":
        webResourceManager.requireResourcesForContext("bar");
        writer = new StringWriter();
        webResourceManager.includeResources(writer);
        resources = writer.toString();
        assertFalse(resources.contains(resourceA + ".css"));
        assertFalse(resources.contains(resourceB + ".css"));
        assertFalse(resources.contains(resourceC + ".css"));
        assertFalse(resources.contains("/contextbatch/css/foo/batch.css"));
        assertTrue(resources.contains("/contextbatch/css/bar/batch.css"));
    }

    public void testGetResourceContextWithCondition() throws ClassNotFoundException, DocumentException
    {
        when(mockBatchingConfiguration.isContextBatchingEnabled()).thenReturn(true);

        final String resource1 = "test.atlassian:cool-stuff";
        final String resource2 = "test.atlassian:hot-stuff";
        final Plugin plugin = TestUtils.createTestPlugin("test.atlassian", "1", AlwaysTrueCondition.class, AlwaysFalseCondition.class);
        final WebResourceModuleDescriptor resource1Descriptor = new WebResourceModuleDescriptorBuilder(plugin, "cool-stuff").setCondition(
                AlwaysTrueCondition.class).addDescriptor("cool.js").addContext("foo").build();
        mockEnabledPluginModule(resource1, resource1Descriptor);

        final WebResourceModuleDescriptor resource2Descriptor = new WebResourceModuleDescriptorBuilder(plugin, "hot-stuff").setCondition(
                AlwaysFalseCondition.class).addDescriptor("hot.js").addContext("foo").build();
        mockEnabledPluginModule(resource2, resource2Descriptor);

        final String resource3 = "test.atlassian:warm-stuff";
        final WebResourceModuleDescriptor resourceDescriptor3 = TestUtils.createWebResourceModuleDescriptor(resource3, testPlugin,
                TestUtils.createResourceDescriptors("warm.js"), Collections.<String>emptyList(), new HashSet<String>()
        {
            {
                add("foo");
            }
        });
        mockEnabledPluginModule(resource3, resourceDescriptor3);

        when(mockPluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class)).thenReturn(
                asList(resource1Descriptor, resource2Descriptor, resourceDescriptor3));

        setupRequestCache();
        webResourceManager.requireResourcesForContext("foo");

        final String tags = webResourceManager.getRequiredResources();
        assertTrue(tags.contains(resource1));
        assertFalse(tags.contains(resource2));
        assertTrue(tags.contains("foo/batch.js"));
    }

    private Map<String, Object> setupRequestCache()
    {
        final Map<String, Object> requestCache = new HashMap<String, Object>();
        when(mockWebResourceIntegration.getRequestCache()).thenReturn(requestCache);
        return requestCache;
    }

    public void testRequireResourceWithDuplicateDependencies()
    {
        final String resourceA = "test.atlassian:a";
        final String resourceB = "test.atlassian:b";
        final String resourceC = "test.atlassian:c";
        final String resourceD = "test.atlassian:d";
        final String resourceE = "test.atlassian:e";

        // A depends on B, C
        mockEnabledPluginModule(resourceA, TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin,
                Collections.<ResourceDescriptor>emptyList(), Arrays.asList(resourceB, resourceC)));
        // B depends on D
        mockEnabledPluginModule(resourceB, TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin,
                Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(resourceD)));
        // C depends on E
        mockEnabledPluginModule(resourceC, TestUtils.createWebResourceModuleDescriptor(resourceC, testPlugin,
                Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(resourceE)));
        // D depends on C
        mockEnabledPluginModule(resourceD, TestUtils.createWebResourceModuleDescriptor(resourceD, testPlugin,
                Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(resourceC)));
        // E has no dependencies
        mockEnabledPluginModule(resourceE, TestUtils.createWebResourceModuleDescriptor(resourceE, testPlugin));

        final Map<String, Object> requestCache = setupRequestCache();
        webResourceManager.requireResource(resourceA);

        final Collection<?> resources = (Collection<?>) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(5, resources.size());
        final Object[] resourceArray = resources.toArray();
        assertEquals(resourceE, resourceArray[0]);
        assertEquals(resourceC, resourceArray[1]);
        assertEquals(resourceD, resourceArray[2]);
        assertEquals(resourceB, resourceArray[3]);
        assertEquals(resourceA, resourceArray[4]);
    }

    public void testRequireSingleResourceGetsDeps() throws Exception
    {
        final String resourceA = "test.atlassian:a";
        final String resourceB = "test.atlassian:b";

        final List<ResourceDescriptor> resourceDescriptorsA = TestUtils.createResourceDescriptors("resourceA.css");
        final List<ResourceDescriptor> resourceDescriptorsB = TestUtils.createResourceDescriptors("resourceB.css", "resourceB-more.css");

        // A depends on B
        mockEnabledPluginModule(resourceA, TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, resourceDescriptorsA,
                Collections.singletonList(resourceB)));
        mockEnabledPluginModule(resourceB, TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin, resourceDescriptorsB,
                Collections.<String>emptyList()));

        final String s = webResourceManager.getResourceTags(resourceA);
        final int indexA = s.indexOf(resourceA);
        final int indexB = s.indexOf(resourceB);

        assertNotSame(-1, indexA);
        assertNotSame(-1, indexB);
        assertTrue(indexB < indexA);
    }

    public void testIncludeResourcesWithResourceList() throws Exception
    {
        final String resourceA = "test.atlassian:a";

        final List<ResourceDescriptor> resourceDescriptorsA = TestUtils.createResourceDescriptors("resourceA.css");

        mockEnabledPluginModule(resourceA, TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, resourceDescriptorsA,
                Collections.<String>emptyList()));

        final StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.includeResources(Arrays.<String>asList(resourceA), requiredResourceWriter, UrlMode.ABSOLUTE);
        final String result = requiredResourceWriter.toString();
        assertTrue(result.contains(resourceA));
    }

    public void testIncludeResourcesWithResourceListIgnoresRequireResource() throws Exception
    {
        final String resourceA = "test.atlassian:a";
        final String resourceB = "test.atlassian:b";

        final List<ResourceDescriptor> resourceDescriptorsA = TestUtils.createResourceDescriptors("resourceA.css");
        final List<ResourceDescriptor> resourceDescriptorsB = TestUtils.createResourceDescriptors("resourceB.css", "resourceB-more.css");

        final Map<String, Object> requestCache = new HashMap<String, Object>();
        when(mockWebResourceIntegration.getRequestCache()).thenReturn(requestCache);

        mockEnabledPluginModule(resourceA, TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, resourceDescriptorsA,
                Collections.<String>emptyList()));

        mockEnabledPluginModule(resourceB, TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin, resourceDescriptorsB,
                Collections.<String>emptyList()));

        final StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.requireResource(resourceB);
        webResourceManager.includeResources(Arrays.<String>asList(resourceA), requiredResourceWriter, UrlMode.ABSOLUTE);
        final String result = requiredResourceWriter.toString();
        assertFalse(result.contains(resourceB));
    }

    public void testIncludeResourcesWithResourceListIncludesDependences() throws Exception
    {
        final String resourceA = "test.atlassian:a";
        final String resourceB = "test.atlassian:b";

        final List<ResourceDescriptor> resourceDescriptorsA = TestUtils.createResourceDescriptors("resourceA.css");
        final List<ResourceDescriptor> resourceDescriptorsB = TestUtils.createResourceDescriptors("resourceB.css", "resourceB-more.css");

        // A depends on B
        mockEnabledPluginModule(resourceA, TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, resourceDescriptorsA,
                Collections.singletonList(resourceB)));
        mockEnabledPluginModule(resourceB, TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin, resourceDescriptorsB,
                Collections.<String>emptyList()));

        final StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.includeResources(Arrays.<String>asList(resourceA), requiredResourceWriter, UrlMode.ABSOLUTE);
        final String result = requiredResourceWriter.toString();
        assertTrue(result.contains(resourceB));
    }

    public void testIncludeResourcesWithResourceListIncludesDependencesFromSuperBatch() throws Exception
    {
        final String resourceA = "test.atlassian:a";
        final String resourceB = "test.atlassian:b";

        final ResourceBatchingConfiguration mockBatchingConfiguration = mock(ResourceBatchingConfiguration.class);
        when(mockBatchingConfiguration.isSuperBatchingEnabled()).thenReturn(true);
        when(mockBatchingConfiguration.getSuperBatchModuleCompleteKeys()).thenReturn(Arrays.asList(resourceB));

        webResourceManager = new WebResourceManagerImpl(pluginResourceLocator, mockWebResourceIntegration, mockUrlProvider, mockBatchingConfiguration);

        final List<ResourceDescriptor> resourceDescriptorsA = TestUtils.createResourceDescriptors("resourceA.css");
        final List<ResourceDescriptor> resourceDescriptorsB = TestUtils.createResourceDescriptors("resourceB.css");

        // A depends on B
        mockEnabledPluginModule(resourceA, TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, resourceDescriptorsA,
                Collections.singletonList(resourceB)));
        mockEnabledPluginModule(resourceB, TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin, resourceDescriptorsB,
                Collections.<String>emptyList()));

        final StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.includeResources(Arrays.<String>asList(resourceA), requiredResourceWriter, UrlMode.ABSOLUTE);
        final String result = requiredResourceWriter.toString();
        assertTrue(result.contains(resourceB));
    }

    public void testIncludeResourcesWithSharedDependencies() throws Exception
    {
        final String resourceA = "test.atlassian:a";
        final String resourceB = "test.atlassian:b";
        final String resourceShared = "test.atlassian:c";

        when(mockBatchingConfiguration.getSuperBatchModuleCompleteKeys()).thenReturn(Arrays.asList(resourceB));

        webResourceManager = new WebResourceManagerImpl(pluginResourceLocator, mockWebResourceIntegration, mockUrlProvider, mockBatchingConfiguration);

        final List<ResourceDescriptor> resourceDescriptorsA = TestUtils.createResourceDescriptors("resourceA.css");
        final List<ResourceDescriptor> resourceDescriptorsB = TestUtils.createResourceDescriptors("resourceB.css");
        final List<ResourceDescriptor> resourceDescriptorsShared = TestUtils.createResourceDescriptors("resourceC.css");

        // A depends on C
        mockEnabledPluginModule(resourceA, TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, resourceDescriptorsA,
                Collections.singletonList(resourceShared)));
        // B depends on C
        mockEnabledPluginModule(resourceB, TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin, resourceDescriptorsB,
                Collections.singletonList(resourceShared)));
        mockEnabledPluginModule(resourceShared, TestUtils.createWebResourceModuleDescriptor(resourceShared, testPlugin, resourceDescriptorsShared,
                Collections.<String>emptyList()));

        final StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.includeResources(Arrays.<String>asList(resourceA, resourceB), requiredResourceWriter, UrlMode.ABSOLUTE);
        final String result = requiredResourceWriter.toString();
        assertTrue(result.contains(resourceA));
        assertTrue(result.contains(resourceB));

        Pattern resourceCount = Pattern.compile("/download/batch/test\\.atlassian:c/test\\.atlassian:c.css");
        Matcher m = resourceCount.matcher(result);

        assertTrue(m.find());
        assertFalse(m.find(m.end()));
    }


    public void testRequireResourcesAreClearedAfterIncludesResourcesIsCalled() throws Exception
    {
        final String moduleKey = "cool-resources";
        final String completeModuleKey = "test.atlassian:" + moduleKey;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.css", "more-cool.css", "cool.js");

        setupRequestCache();

        mockEnabledPluginModule(completeModuleKey, TestUtils.createWebResourceModuleDescriptor(completeModuleKey, testPlugin, resourceDescriptors1));

        // test requireResource() methods
        webResourceManager.requireResource(completeModuleKey);
        webResourceManager.includeResources(new StringWriter(), UrlMode.RELATIVE);

        final StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.includeResources(requiredResourceWriter, UrlMode.RELATIVE);
        assertEquals("", requiredResourceWriter.toString());
    }

    // testRequireResourceAndResourceTagMethods

    public void testRequireResourceAndResourceTagMethods() throws Exception
    {
        final String completeModuleKey = "test.atlassian:cool-resources";
        final String staticBase = setupRequireResourceAndResourceTagMethods(false, completeModuleKey);

        // test requireResource() methods
        final StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey);
        final String requiredResourceResult = webResourceManager.getRequiredResources(UrlMode.RELATIVE);
        webResourceManager.includeResources(requiredResourceWriter, UrlMode.RELATIVE);
        assertEquals(requiredResourceResult, requiredResourceWriter.toString());

        assertTrue(requiredResourceResult.contains("href=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".css"));
        assertTrue(requiredResourceResult.contains("src=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".js"));

        // test resourceTag() methods
        final StringWriter resourceTagsWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey, resourceTagsWriter, UrlMode.RELATIVE);
        final String resourceTagsResult = webResourceManager.getResourceTags(completeModuleKey, UrlMode.RELATIVE);
        assertEquals(resourceTagsResult, resourceTagsWriter.toString());

        // calling requireResource() or resourceTag() on a single webresource should be the same
        assertEquals(requiredResourceResult, resourceTagsResult);
    }

    public void testRequireResourceAndResourceTagMethodsWithAbsoluteUrlMode() throws Exception
    {
        testRequireResourceAndResourceTagMethods(UrlMode.ABSOLUTE, true);
    }

    public void testRequireResourceAndResourceTagMethodsWithRelativeUrlMode() throws Exception
    {
        testRequireResourceAndResourceTagMethods(UrlMode.RELATIVE, false);
    }

    public void testRequireResourceAndResourceTagMethodsWithAutoUrlMode() throws Exception
    {
        testRequireResourceAndResourceTagMethods(UrlMode.AUTO, false);
    }

    private void testRequireResourceAndResourceTagMethods(final UrlMode urlMode, final boolean baseUrlExpected) throws Exception
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
        final StringWriter resourceTagsWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey, resourceTagsWriter, urlMode);
        final String resourceTagsResult = webResourceManager.getResourceTags(completeModuleKey, urlMode);
        assertEquals(resourceTagsResult, resourceTagsWriter.toString());

        // calling requireResource() or resourceTag() on a single webresource should be the same
        assertEquals(requiredResourceResult, resourceTagsResult);
    }

    private String setupRequireResourceAndResourceTagMethods(final boolean baseUrlExpected, final String completeModuleKey) throws DocumentException
    {
        final List<ResourceDescriptor> descriptors = TestUtils.createResourceDescriptors("cool.css", "more-cool.css", "cool.js");

        final Map<String, Object> requestCache = new HashMap<String, Object>();
        when(mockWebResourceIntegration.getRequestCache()).thenReturn(requestCache);

        mockEnabledPluginModule(completeModuleKey, TestUtils.createWebResourceModuleDescriptor(completeModuleKey, testPlugin, descriptors));

        final String staticPrefix = (baseUrlExpected ? BASEURL : "") + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + testPlugin.getPluginInformation().getVersion() + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
        when(mockUrlProvider.getStaticResourcePrefix(eq("1"), isA(UrlMode.class))).thenReturn(staticPrefix);
        return staticPrefix + BatchPluginResource.URL_PREFIX;
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
        testRequireResourceWithCacheParameter(UrlMode.ABSOLUTE, true);
    }

    public void testRequireResourceWithCacheParameterAndRelativeUrlMode() throws Exception
    {
        testRequireResourceWithCacheParameter(UrlMode.RELATIVE, false);
    }

    public void testRequireResourceWithCacheParameterAndAutoUrlMode() throws Exception
    {
        testRequireResourceWithCacheParameter(UrlMode.AUTO, false);
    }

    private void testRequireResourceWithCacheParameter(final UrlMode urlMode, final boolean baseUrlExpected) throws Exception
    {
        final String completeModuleKey = "test.atlassian:no-cache-resources";
        final String expectedResult = setupRequireResourceWithCacheParameter(baseUrlExpected, completeModuleKey);
        assertTrue(webResourceManager.getResourceTags(completeModuleKey, urlMode).contains(expectedResult));
    }

    private String setupRequireResourceWithCacheParameter(final boolean baseUrlExpected, final String completeModuleKey) throws DocumentException
    {
        final Map<String, String> params = new HashMap<String, String>();
        params.put("cache", "false");
        final ResourceDescriptor resourceDescriptor = TestUtils.createResourceDescriptor("no-cache.js", params);

        mockEnabledPluginModule(completeModuleKey, TestUtils.createWebResourceModuleDescriptor(completeModuleKey, testPlugin,
                Collections.singletonList(resourceDescriptor)));

        return "src=\"" + (baseUrlExpected ? BASEURL : "") + BatchPluginResource.URL_PREFIX + "/" + completeModuleKey + "/" + completeModuleKey + ".js?cache=false";
    }

    public void testGetRequiredResourcesWithFilter() throws Exception
    {
        final String moduleKey = "cool-resources";
        final String completeModuleKey = "test.atlassian:" + moduleKey;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.css", "cool.js", "more-cool.css");

        setupRequestCache();

        mockEnabledPluginModule(completeModuleKey, TestUtils.createWebResourceModuleDescriptor(completeModuleKey, testPlugin, resourceDescriptors1));

        // test includeResources(writer, type) method
        webResourceManager.requireResource(completeModuleKey);

        final String staticPrefix = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + testPlugin.getPluginInformation().getVersion() + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
        when(mockUrlProvider.getStaticResourcePrefix("1", UrlMode.ABSOLUTE)).thenReturn(staticPrefix);
        final String staticBase = staticPrefix + BatchPluginResource.URL_PREFIX;

        final String cssRef = "href=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".css";
        final String jsRef = "src=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".js";

        // CSS
        String requiredResourceResult = webResourceManager.getRequiredResources(UrlMode.ABSOLUTE, new CssWebResource());
        assertTrue(requiredResourceResult.contains(cssRef));
        assertFalse(requiredResourceResult.contains(jsRef));

        // JS
        requiredResourceResult = webResourceManager.getRequiredResources(UrlMode.ABSOLUTE, new JavascriptWebResource());
        assertFalse(requiredResourceResult.contains(cssRef));
        assertTrue(requiredResourceResult.contains(jsRef));

        // BOTH
        requiredResourceResult = webResourceManager.getRequiredResources(UrlMode.ABSOLUTE);
        assertTrue(requiredResourceResult.contains(cssRef));
        assertTrue(requiredResourceResult.contains(jsRef));
    }

    public void testGetRequiredResourcesWithCustomFilters() throws Exception
    {
        final WebResourceFilter atlassianFilter = new WebResourceFilter()
        {
            public boolean matches(final String resourceName)
            {
                return resourceName.contains("atlassian");
            }
        };
        final WebResourceFilter bogusFilter = new WebResourceFilter()
        {
            public boolean matches(final String resourceName)
            {
                return true;
            }
        };

        final String moduleKey = "cool-resources";
        final String completeModuleKey = "test.atlassian:" + moduleKey;

        final List<ResourceDescriptor> resources = TestUtils.createResourceDescriptors("foo.css", "foo-bar.js", "atlassian.css",
                "atlassian-plugins.js");

        setupRequestCache();
        mockEnabledPluginModule(completeModuleKey, TestUtils.createWebResourceModuleDescriptor(completeModuleKey, testPlugin, resources));

        // easier to test which resources were included by the filter with batching turned off
        when(mockBatchingConfiguration.isPluginWebResourceBatchingEnabled()).thenReturn(false);
        webResourceManager.requireResource(completeModuleKey);
        final String atlassianResources = webResourceManager.getRequiredResources(UrlMode.RELATIVE, atlassianFilter);
        assertEquals(-1, atlassianResources.indexOf("foo"));
        assertTrue(atlassianResources.contains("atlassian.css"));
        assertTrue(atlassianResources.contains("atlassian-plugins.js"));

        final String allResources = webResourceManager.getRequiredResources(UrlMode.RELATIVE, bogusFilter);
        for (final ResourceDescriptor resource : resources)
        {
            assertTrue(allResources.contains(resource.getName()));
        }
    }

    public void testGetRequiredResourcesOrdersByType() throws Exception
    {
        final String moduleKey1 = "cool-resources";
        final String moduleKey2 = "hot-resources";
        final String completeModuleKey1 = "test.atlassian:" + moduleKey1;
        final String completeModuleKey2 = "test.atlassian:" + moduleKey2;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.js", "cool.css", "more-cool.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("hot.js", "hot.css", "more-hot.css");

        final Plugin plugin = TestUtils.createTestPlugin();

        setupRequestCache();

        mockEnabledPluginModule(completeModuleKey1, TestUtils.createWebResourceModuleDescriptor(completeModuleKey1, plugin, resourceDescriptors1));
        mockEnabledPluginModule(completeModuleKey2, TestUtils.createWebResourceModuleDescriptor(completeModuleKey2, plugin, resourceDescriptors2));

        // test includeResources(writer, type) method
        webResourceManager.requireResource(completeModuleKey1);
        webResourceManager.requireResource(completeModuleKey2);

        final String staticPrefix = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + plugin.getPluginInformation().getVersion() + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
        when(mockUrlProvider.getStaticResourcePrefix("1", UrlMode.ABSOLUTE)).thenReturn(staticPrefix);
        final String staticBase = staticPrefix + BatchPluginResource.URL_PREFIX;

        final String cssRef1 = "href=\"" + staticBase + "/" + completeModuleKey1 + "/" + completeModuleKey1 + ".css";
        final String cssRef2 = "href=\"" + staticBase + "/" + completeModuleKey2 + "/" + completeModuleKey2 + ".css";
        final String jsRef1 = "src=\"" + staticBase + "/" + completeModuleKey1 + "/" + completeModuleKey1 + ".js";
        final String jsRef2 = "src=\"" + staticBase + "/" + completeModuleKey2 + "/" + completeModuleKey2 + ".js";

        final String requiredResourceResult = webResourceManager.getRequiredResources(UrlMode.ABSOLUTE);

        assertTrue(requiredResourceResult.contains(cssRef1));
        assertTrue(requiredResourceResult.contains(cssRef2));
        assertTrue(requiredResourceResult.contains(jsRef1));
        assertTrue(requiredResourceResult.contains(jsRef2));

        final int cssRef1Index = requiredResourceResult.indexOf(cssRef1);
        final int cssRef2Index = requiredResourceResult.indexOf(cssRef2);
        final int jsRef1Index = requiredResourceResult.indexOf(jsRef1);
        final int jsRef2Index = requiredResourceResult.indexOf(jsRef2);

        assertTrue(cssRef1Index < jsRef1Index);
        assertTrue(cssRef2Index < jsRef2Index);
        assertTrue(cssRef2Index < jsRef1Index);
    }

    public void testRequireResourceInSuperbatch() throws ClassNotFoundException
    {
        setupSuperBatch();

        final Map<String, Object> requestCache = setupRequestCache();
        mockOutSuperbatchPluginAccesses();

        webResourceManager.requireResource("test.atlassian:superbatch");

        final Collection<?> resources = (Collection<?>) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(0, resources.size());
    }

    public void testRequireResourceWithDependencyInSuperbatch() throws DocumentException, ClassNotFoundException
    {
        setupSuperBatch();

        mockOutSuperbatchPluginAccesses();

        final Map<String, Object> requestCache = setupRequestCache();

        mockEnabledPluginModule("test.atlassian:included-resource", TestUtils.createWebResourceModuleDescriptor("test.atlassian:included-resource",
                testPlugin, Collections.<ResourceDescriptor>emptyList(), Collections.singletonList("test.atlassian:superbatch")));

        webResourceManager.requireResource("test.atlassian:included-resource");

        final Collection<?> resources = (Collection<?>) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(1, resources.size());
        assertEquals("test.atlassian:included-resource", resources.iterator().next());
    }

    public void testSuperBatchResolution() throws DocumentException, ClassNotFoundException
    {
        setupSuperBatch();

        TestUtils.setupSuperbatchTestContent(mockPluginAccessor, testPlugin);

        final List<PluginResource> cssResources = webResourceManager.getSuperBatchResources(CssWebResource.FORMATTER);
        assertEquals(2, cssResources.size());

        final SuperBatchPluginResource superBatch1 = (SuperBatchPluginResource) cssResources.get(0);
        assertEquals("batch.css", superBatch1.getResourceName());
        assertTrue(superBatch1.getParams().isEmpty());

        final SuperBatchPluginResource superBatch2 = (SuperBatchPluginResource) cssResources.get(1);
        assertEquals("batch.css", superBatch2.getResourceName());
        assertEquals("true", superBatch2.getParams().get("ieonly"));

        final List<PluginResource> jsResources = webResourceManager.getSuperBatchResources(JavascriptWebResource.FORMATTER);
        assertEquals(1, jsResources.size());
        assertEquals("batch.js", jsResources.get(0).getResourceName());
        assertEquals(0, jsResources.get(0).getParams().size());
    }

    // First part: execute some WRM code on in a new context on top of a non-empty context and write out.
    //             Check that the expected resources are included in the inner write out and that the outer resources aren't included
    // Second part: after the inner execution is completed, write the outer resources out, checking that we didn't get the inner resources and that we did get
    //              the resources which were in the context before the inner execution.
    public void testNestedContextExclusivity() throws Exception
    {

        final String moduleKey1 = "cool-resources";
        final String moduleKey2 = "hot-resources";
        final String completeModuleKey1 = "test.atlassian:" + moduleKey1;
        final String completeModuleKey2 = "test.atlassian:" + moduleKey2;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.js", "cool.css", "more-cool.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("hot.js", "hot.css", "more-hot.css");

        final Plugin plugin = TestUtils.createTestPlugin();

        setupRequestCache();

        mockEnabledPluginModule(completeModuleKey1, TestUtils.createWebResourceModuleDescriptor(completeModuleKey1, plugin, resourceDescriptors1));
        mockEnabledPluginModule(completeModuleKey2, TestUtils.createWebResourceModuleDescriptor(completeModuleKey2, plugin, resourceDescriptors2));

        final String cssRef1 = completeModuleKey1 + "/" + completeModuleKey1 + ".css";
        final String cssRef2 = completeModuleKey2 + "/" + completeModuleKey2 + ".css";
        final String jsRef1 = completeModuleKey1 + "/" + completeModuleKey1 + ".js";
        final String jsRef2 = completeModuleKey2 + "/" + completeModuleKey2 + ".js";

        // test includeResources(writer, type) method
        webResourceManager.requireResource(completeModuleKey1);
        webResourceManager.executeInNewContext(new Supplier<Void>()
        {
            public Void get()
            {
                webResourceManager.requireResource(completeModuleKey2);

                StringWriter writer = new StringWriter();
                webResourceManager.includeResources(writer);
                String resources = writer.toString();

                assertFalse(resources.contains(cssRef1));
                assertTrue(resources.contains(cssRef2));

                assertFalse(resources.contains(jsRef1));
                assertTrue(resources.contains(jsRef2));
                return null;
            }
        });

        StringWriter writer = new StringWriter();
        webResourceManager.includeResources(writer);
        String resources = writer.toString();

        assertTrue(resources.contains(cssRef1));
        assertFalse(resources.contains(cssRef2));

        assertTrue(resources.contains(jsRef1));
        assertFalse(resources.contains(jsRef2));

    }

    // Require and include some resource in an inner context with an empty outer context. Check that the inner context gets the expected resources.
    // After the inner code is executed, check that we didn't get any resources in the outer context
    public void testNestedContextOnEmptyBase() throws Exception
    {
        final String moduleKey1 = "cool-resources";
        final String completeModuleKey1 = "test.atlassian:" + moduleKey1;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.js", "cool.css", "more-cool.css");

        final Plugin plugin = TestUtils.createTestPlugin();

        setupRequestCache();

        mockEnabledPluginModule(completeModuleKey1, TestUtils.createWebResourceModuleDescriptor(completeModuleKey1, plugin, resourceDescriptors1));

        final String cssRef1 = completeModuleKey1 + "/" + completeModuleKey1 + ".css";
        final String jsRef1 = completeModuleKey1 + "/" + completeModuleKey1 + ".js";

        // test includeResources(writer, type) method
        webResourceManager.executeInNewContext(new Supplier<Void>()
        {
            public Void get()
            {
                webResourceManager.requireResource(completeModuleKey1);

                StringWriter writer = new StringWriter();
                webResourceManager.includeResources(writer);
                String resources = writer.toString();

                assertTrue(resources.contains(cssRef1));
                assertTrue(resources.contains(jsRef1));

                return null;
            }
        });

        StringWriter writer = new StringWriter();
        webResourceManager.includeResources(writer);
        String resources = writer.toString();

        assertFalse(resources.contains(cssRef1));
        assertFalse(resources.contains(jsRef1));
    }

    // Require (but not include) some resource in an inner context with an empty outer context. After the inner code is executed,
    // check that we didn't get any resources in the outer context
    public void testNestedContextWithoutWriting() throws Exception
    {
        final String moduleKey1 = "cool-resources";
        final String completeModuleKey1 = "test.atlassian:" + moduleKey1;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.js", "cool.css", "more-cool.css");

        final Plugin plugin = TestUtils.createTestPlugin();

        setupRequestCache();

        mockEnabledPluginModule(completeModuleKey1, TestUtils.createWebResourceModuleDescriptor(completeModuleKey1, plugin, resourceDescriptors1));

        final String cssRef1 = completeModuleKey1 + "/" + completeModuleKey1 + ".css";
        final String jsRef1 = completeModuleKey1 + "/" + completeModuleKey1 + ".js";

        // test includeResources(writer, type) method
        webResourceManager.executeInNewContext(new Supplier<Void>()
        {
            public Void get()
            {
                webResourceManager.requireResource(completeModuleKey1);
                return null;
            }
        });

        StringWriter writer = new StringWriter();
        webResourceManager.includeResources(writer);
        String resources = writer.toString();

        // check that we didn't get these resources (meaning that the require resources call never caused the module to be included)
        assertFalse(resources.contains(cssRef1));
        assertFalse(resources.contains(jsRef1));
    }

    // push content into the outer-most context, nest multiple non-empty executeInNewContext calls, ensuring that the expected content is
    // written in each context
    public void testMultipleNestedContexts() throws Exception
    {
        final String moduleKey1 = "cool-resources";
        final String moduleKey2 = "hot-resources";
        final String moduleKey3 = "warm-resources";
        final String completeModuleKey1 = "test.atlassian:" + moduleKey1;
        final String completeModuleKey2 = "test.atlassian:" + moduleKey2;
        final String completeModuleKey3 = "test.atlassian:" + moduleKey3;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.js", "cool.css", "more-cool.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("hot.js", "hot.css", "more-hot.css");
        final List<ResourceDescriptor> resourceDescriptors3 = TestUtils.createResourceDescriptors("warm.js", "warm.css", "more-warm.css");

        final Plugin plugin = TestUtils.createTestPlugin();

        setupRequestCache();

        mockEnabledPluginModule(completeModuleKey1, TestUtils.createWebResourceModuleDescriptor(completeModuleKey1, plugin, resourceDescriptors1));
        mockEnabledPluginModule(completeModuleKey2, TestUtils.createWebResourceModuleDescriptor(completeModuleKey2, plugin, resourceDescriptors2));
        mockEnabledPluginModule(completeModuleKey3, TestUtils.createWebResourceModuleDescriptor(completeModuleKey3, plugin, resourceDescriptors3));

        final String cssRef1 = completeModuleKey1 + "/" + completeModuleKey1 + ".css";
        final String cssRef2 = completeModuleKey2 + "/" + completeModuleKey2 + ".css";
        final String cssRef3 = completeModuleKey3 + "/" + completeModuleKey3 + ".css";
        final String jsRef1 = completeModuleKey1 + "/" + completeModuleKey1 + ".js";
        final String jsRef2 = completeModuleKey2 + "/" + completeModuleKey2 + ".js";
        final String jsRef3 = completeModuleKey3 + "/" + completeModuleKey3 + ".js";

        setupRequestCache();

        // require module 1 for outermost resource
        webResourceManager.requireResource(completeModuleKey1);

        // nest middle resource context (requests module 2)
        webResourceManager.executeInNewContext(new Supplier<Void>()
        {
            public Void get()
            {
                webResourceManager.requireResource(completeModuleKey2);
                // nest inner-most resource context (requests module 3)
                webResourceManager.executeInNewContext(new Supplier<Void>()
                {
                    public Void get()
                    {
                        webResourceManager.requireResource(completeModuleKey3);

                        // check that the inner-most context only got module 3
                        StringWriter writer = new StringWriter();
                        webResourceManager.includeResources(writer);
                        String resources = writer.toString();

                        assertTrue(resources.contains(cssRef3));
                        assertTrue(resources.contains(jsRef3));
                        assertFalse(resources.contains(cssRef1));
                        assertFalse(resources.contains(jsRef1));
                        assertFalse(resources.contains(cssRef2));
                        assertFalse(resources.contains(jsRef2));

                        return null;
                    }
                });

                // check that the middle context only got module 2
                StringWriter writer = new StringWriter();
                webResourceManager.includeResources(writer);
                String resources = writer.toString();

                assertTrue(resources.contains(cssRef2));
                assertTrue(resources.contains(jsRef2));
                assertFalse(resources.contains(cssRef1));
                assertFalse(resources.contains(jsRef1));
                assertFalse(resources.contains(cssRef3));
                assertFalse(resources.contains(jsRef3));

                return null;
            }
        });

        // check that the outer context only got module 1

        StringWriter writer = new StringWriter();
        webResourceManager.includeResources(writer);
        String resources = writer.toString();

        assertTrue(resources.contains(cssRef1));
        assertTrue(resources.contains(jsRef1));
        assertFalse(resources.contains(cssRef2));
        assertFalse(resources.contains(jsRef2));
        assertFalse(resources.contains(cssRef3));
        assertFalse(resources.contains(jsRef3));
    }

    private void setupSuperBatch()
    {
        when(mockBatchingConfiguration.isSuperBatchingEnabled()).thenReturn(true);
        when(mockBatchingConfiguration.getSuperBatchModuleCompleteKeys()).thenReturn(Arrays.asList(
                "test.atlassian:superbatch",
                "test.atlassian:superbatch2",
                "test.atlassian:missing-plugin"
        ));
    }

    private void mockOutSuperbatchPluginAccesses() throws ClassNotFoundException
    {
        mockOutPluginModule("test.atlassian:superbatch");
        mockOutPluginModule("test.atlassian:superbatch2");
        when(mockPluginAccessor.getPluginModule("test.atlassian:missing-plugin")).thenReturn(null);
        when(mockPluginAccessor.getEnabledPluginModule("test.atlassian:missing-plugin")).thenReturn(null);
    }

    private void mockOutPluginModule(final String moduleKey) throws ClassNotFoundException
    {
        final Plugin p = TestUtils.createTestPlugin();
        final ModuleDescriptor module = TestUtils.createWebResourceModuleDescriptor(moduleKey, p);
        when(mockPluginAccessor.getPluginModule(moduleKey)).thenReturn(module);
        when(mockPluginAccessor.getEnabledPluginModule(moduleKey)).thenReturn(module);
    }
}
