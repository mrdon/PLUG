package com.atlassian.plugin.webresource;

import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Iterables.size;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableClasspathResource;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.servlet.ForwardableResource;
import com.atlassian.plugin.servlet.ServletContextFactory;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformerModuleDescriptor;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.TestCase;

public class TestPluginResourceLocatorImpl extends TestCase
{
    private PluginResourceLocatorImpl pluginResourceLocator;
    @Mock
    private WebResourceIntegration mockWebResourceIntegration;
    @Mock
    private PluginAccessor mockPluginAccessor;
    @Mock
    private ServletContextFactory mockServletContextFactory;
    @Mock
    private ResourceBatchingConfiguration mockBatchingConfiguration;
    @Mock
    private WebResourceUrlProvider mockWebResourceUrlProvider;

    private static final String TEST_PLUGIN_KEY = "test.plugin";
    private static final String TEST_MODULE_KEY = "web-resources";
    private static final String TEST_MODULE_COMPLETE_KEY = TEST_PLUGIN_KEY + ":" + TEST_MODULE_KEY;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        MockitoAnnotations.initMocks(this);
        when(mockWebResourceIntegration.getPluginAccessor()).thenReturn(mockPluginAccessor);

        pluginResourceLocator = new PluginResourceLocatorImpl(mockWebResourceIntegration, mockServletContextFactory, mockWebResourceUrlProvider,
            mockBatchingConfiguration);
    }

    @Override
    protected void tearDown() throws Exception
    {
        pluginResourceLocator = null;
        mockWebResourceIntegration = null;
        mockPluginAccessor = null;
        mockServletContextFactory = null;
        mockBatchingConfiguration = null;
        mockWebResourceUrlProvider = null;

        super.tearDown();
    }

    public void testMatches()
    {
        assertTrue(pluginResourceLocator.matches("/download/superbatch/css/batch.css"));
        assertTrue(pluginResourceLocator.matches("/download/superbatch/css/images/background/blah.gif"));
        assertTrue(pluginResourceLocator.matches("/download/batch/plugin.key:module-key/plugin.key.js"));
        assertTrue(pluginResourceLocator.matches("/download/resources/plugin.key:module-key/foo.png"));
        assertTrue(pluginResourceLocator.matches("/download/contextbatch/css/contexta/batch.css"));
        assertTrue(pluginResourceLocator.matches("/download/contextbatch/css/contexta/images/blah.png"));
    }

    public void testNotMatches()
    {
        assertFalse(pluginResourceLocator.matches("/superbatch/batch.css"));
        assertFalse(pluginResourceLocator.matches("/download/blah.css"));
    }

    public void testGetAndParseUrl()
    {
        final SinglePluginResource resource = new SinglePluginResource("plugin.key:my-resources", "foo.css", false);
        final String url = resource.getUrl();
        assertTrue(pluginResourceLocator.matches(url));
    }

    public void testGetPluginResourcesWithoutBatching() throws Exception
    {
        final Plugin mockPlugin = mock(Plugin.class);
        when(mockPlugin.getPluginsVersion()).thenReturn(1);

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("master-ie.css", "master.css", "comments.css");

        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn(
            (ModuleDescriptor) TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY, mockPlugin, resourceDescriptors));

        when(mockBatchingConfiguration.isPluginWebResourceBatchingEnabled()).thenReturn(false);
        final List<PluginResource> resources = pluginResourceLocator.getPluginResources(TEST_MODULE_COMPLETE_KEY);
        assertEquals(3, resources.size());
        // ensure the resources still have their parameters
        for (final PluginResource resource : resources)
        {
            if (resource.getResourceName().contains("ie"))
            {
                assertEquals("true", resource.getParams().get("ieonly"));
            }
            else
            {
                assertNull(resource.getParams().get("ieonly"));
            }
        }
    }

    public void testGetPluginResourcesWithBatching() throws Exception
    {
        final Plugin mockPlugin = mock(Plugin.class);
        when(mockPlugin.getPluginsVersion()).thenReturn(1);

        when(mockBatchingConfiguration.isPluginWebResourceBatchingEnabled()).thenReturn(true);

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("master-ie.css", "master.css", "comments.css");

        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn(
            (ModuleDescriptor) TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY, mockPlugin, resourceDescriptors));

        final List<PluginResource> resources = pluginResourceLocator.getPluginResources(TEST_MODULE_COMPLETE_KEY);
        assertEquals(2, resources.size());

        final BatchPluginResource ieBatch = (BatchPluginResource) resources.get(0);
        assertEquals(TEST_MODULE_COMPLETE_KEY, ieBatch.getModuleCompleteKey());
        assertEquals("css", ieBatch.getType());
        assertEquals(1, ieBatch.getParams().size());
        assertEquals("true", ieBatch.getParams().get("ieonly"));

        final BatchPluginResource batch = (BatchPluginResource) resources.get(1);
        assertEquals(TEST_MODULE_COMPLETE_KEY, batch.getModuleCompleteKey());
        assertEquals("css", batch.getType());
        assertEquals(0, batch.getParams().size());
    }

    public void testGetPluginResourcesWithBatchParameter() throws Exception
    {
        final Plugin mockPlugin = mock(Plugin.class);
        when(mockPlugin.getPluginsVersion()).thenReturn(1);

        when(mockBatchingConfiguration.isPluginWebResourceBatchingEnabled()).thenReturn(true);

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("master.css", "comments.css");
        final Map<String, String> nonBatchParams = new TreeMap<String, String>();
        nonBatchParams.put("batch", "false");
        resourceDescriptors.add(TestUtils.createResourceDescriptor("nonbatch.css", nonBatchParams));

        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn(
            (ModuleDescriptor) TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY, mockPlugin, resourceDescriptors));

        final List<PluginResource> resources = pluginResourceLocator.getPluginResources(TEST_MODULE_COMPLETE_KEY);
        assertEquals(2, resources.size());

        final BatchPluginResource batch = (BatchPluginResource) resources.get(0);
        assertEquals(TEST_MODULE_COMPLETE_KEY, batch.getModuleCompleteKey());
        assertEquals("css", batch.getType());
        assertEquals(0, batch.getParams().size());

        final SinglePluginResource single = (SinglePluginResource) resources.get(1);
        assertEquals(TEST_MODULE_COMPLETE_KEY, single.getModuleCompleteKey());
    }

    public void testGetPluginResourcesWithForwarding() throws Exception
    {
        final Plugin mockPlugin = mock(Plugin.class);
        when(mockPlugin.getPluginsVersion()).thenReturn(1);

        when(mockBatchingConfiguration.isPluginWebResourceBatchingEnabled()).thenReturn(true);

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("master.css", "comments.css");
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("source", "webContext");
        resourceDescriptors.add(TestUtils.createResourceDescriptor("forward.css", params));

        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn(
            (ModuleDescriptor) TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY, mockPlugin, resourceDescriptors));

        final List<PluginResource> resources = pluginResourceLocator.getPluginResources(TEST_MODULE_COMPLETE_KEY);
        assertEquals(2, resources.size());

        final BatchPluginResource batch = (BatchPluginResource) resources.get(0);
        assertEquals(TEST_MODULE_COMPLETE_KEY, batch.getModuleCompleteKey());
        assertEquals("css", batch.getType());
        assertEquals(0, batch.getParams().size());

        final SinglePluginResource single = (SinglePluginResource) resources.get(1);
        assertEquals(TEST_MODULE_COMPLETE_KEY, single.getModuleCompleteKey());
    }

    public void testGetForwardableResource() throws Exception
    {
        final String resourceName = "test.css";
        final String url = "/download/resources/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("source", "webContext");

        final Plugin mockPlugin = mock(Plugin.class);
        final ModuleDescriptor mockModuleDescriptor = mock(ModuleDescriptor.class);
        when(mockModuleDescriptor.getPluginKey()).thenReturn(TEST_PLUGIN_KEY);
        when(mockModuleDescriptor.getResourceLocation("download", resourceName)).thenReturn(
            new ResourceLocation("", resourceName, "download", "text/css", "", params));

        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn(mockModuleDescriptor);
        when(mockPluginAccessor.getPlugin(TEST_PLUGIN_KEY)).thenReturn(mockPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String> emptyMap());

        assertTrue(resource instanceof ForwardableResource);
    }

    public void testGetDownloadableClasspathResource() throws Exception
    {
        final String resourceName = "test.css";
        final String url = "/download/resources/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;

        final Plugin mockPlugin = mock(Plugin.class);
        final ModuleDescriptor mockModuleDescriptor = mock(ModuleDescriptor.class);
        when(mockModuleDescriptor.getPluginKey()).thenReturn(TEST_PLUGIN_KEY);
        when(mockModuleDescriptor.getResourceLocation("download", resourceName)).thenReturn(
            new ResourceLocation("", resourceName, "download", "text/css", "", Collections.<String, String> emptyMap()));

        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn(mockModuleDescriptor);
        when(mockPluginAccessor.getPlugin(TEST_PLUGIN_KEY)).thenReturn(mockPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String> emptyMap());

        assertTrue(resource instanceof DownloadableClasspathResource);
    }

    public void testGetTransformedDownloadableClasspathResource() throws Exception
    {
        final String resourceName = "test.js";
        final String url = "/download/resources/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;

        final DownloadableResource transformedResource = mock(DownloadableResource.class);
        final WebResourceTransformation trans = new WebResourceTransformation(DocumentHelper.parseText(
            "<transformation extension=\"js\">\n" + "<transformer key=\"foo\" />\n" + "</transformation>").getRootElement());
        final WebResourceTransformer transformer = new WebResourceTransformer()
        {
            public DownloadableResource transform(final Element configElement, final ResourceLocation location, final String filePath, final DownloadableResource nextResource)
            {
                return transformedResource;
            }
        };

        final WebResourceTransformerModuleDescriptor transDescriptor = mock(WebResourceTransformerModuleDescriptor.class);
        when(transDescriptor.getKey()).thenReturn("foo");
        when(transDescriptor.getModule()).thenReturn(transformer);

        final Plugin mockPlugin = mock(Plugin.class);
        final WebResourceModuleDescriptor descriptor = mock(WebResourceModuleDescriptor.class);
        when(descriptor.getPluginKey()).thenReturn(TEST_PLUGIN_KEY);
        when(descriptor.getResourceLocation("download", resourceName)).thenReturn(
            new ResourceLocation("", resourceName, "download", "text/css", "", Collections.<String, String> emptyMap()));
        when(descriptor.getTransformations()).thenReturn(Arrays.asList(trans));

        when(mockPluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceTransformerModuleDescriptor.class)).thenReturn(
            Arrays.asList(transDescriptor));
        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn((ModuleDescriptor) descriptor);
        when(mockPluginAccessor.getPlugin(TEST_PLUGIN_KEY)).thenReturn(mockPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String> emptyMap());

        assertTrue(resource == transformedResource);
    }

    public void testGetUnmatchedTransformDownloadableClasspathResource() throws Exception
    {
        final String resourceName = "test.css";
        final String url = "/download/resources/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;

        final DownloadableResource transformedResource = mock(DownloadableResource.class);
        final WebResourceTransformation trans = new WebResourceTransformation(DocumentHelper.parseText(
            "<transformation extension=\"js\">\n" + "<transformer key=\"foo\" />\n" + "</transformation>").getRootElement());
        final WebResourceTransformer transformer = new WebResourceTransformer()
        {
            public DownloadableResource transform(final Element configElement, final ResourceLocation location, final String extraPath, final DownloadableResource nextResource)
            {
                return transformedResource;
            }
        };

        final WebResourceTransformerModuleDescriptor transDescriptor = mock(WebResourceTransformerModuleDescriptor.class);
        when(transDescriptor.getKey()).thenReturn("foo");
        when(transDescriptor.getModule()).thenReturn(transformer);

        final Plugin mockPlugin = mock(Plugin.class);
        final WebResourceModuleDescriptor descriptor = mock(WebResourceModuleDescriptor.class);
        when(descriptor.getPluginKey()).thenReturn(TEST_PLUGIN_KEY);
        when(descriptor.getResourceLocation("download", resourceName)).thenReturn(
            new ResourceLocation("", resourceName, "download", "text/css", "", Collections.<String, String> emptyMap()));
        when(descriptor.getTransformations()).thenReturn(Arrays.asList(trans));

        when(mockPluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceTransformerModuleDescriptor.class)).thenReturn(
            Arrays.asList(transDescriptor));
        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn((ModuleDescriptor) descriptor);
        when(mockPluginAccessor.getPlugin(TEST_PLUGIN_KEY)).thenReturn(mockPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String> emptyMap());

        assertTrue(resource instanceof DownloadableClasspathResource);
    }

    public void testGetMissingTransformerDownloadableClasspathResource() throws Exception
    {
        final String resourceName = "test.css";
        final String url = "/download/resources/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;

        final DownloadableResource transformedResource = mock(DownloadableResource.class);
        final WebResourceTransformation trans = new WebResourceTransformation(DocumentHelper.parseText(
            "<transformation extension=\"js\">\n" + "<transformer key=\"foo\" />\n" + "</transformation>").getRootElement());
        final WebResourceTransformer transformer = new WebResourceTransformer()
        {
            public DownloadableResource transform(final Element configElement, final ResourceLocation location, final String extraPath, final DownloadableResource nextResource)
            {
                return transformedResource;
            }
        };

        final WebResourceTransformerModuleDescriptor transDescriptor = mock(WebResourceTransformerModuleDescriptor.class);
        when(transDescriptor.getKey()).thenReturn("bar");
        when(transDescriptor.getModule()).thenReturn(transformer);

        final Plugin mockPlugin = mock(Plugin.class);
        final WebResourceModuleDescriptor descriptor = mock(WebResourceModuleDescriptor.class);
        when(descriptor.getPluginKey()).thenReturn(TEST_PLUGIN_KEY);
        when(descriptor.getResourceLocation("download", resourceName)).thenReturn(
            new ResourceLocation("", resourceName, "download", "text/css", "", Collections.<String, String> emptyMap()));
        when(descriptor.getTransformations()).thenReturn(Arrays.asList(trans));

        when(mockPluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceTransformerModuleDescriptor.class)).thenReturn(
            Arrays.asList(transDescriptor));
        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn((ModuleDescriptor) descriptor);
        when(mockPluginAccessor.getPlugin(TEST_PLUGIN_KEY)).thenReturn(mockPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String> emptyMap());

        assertTrue(resource instanceof DownloadableClasspathResource);
    }

    public void testGetDownloadableBatchResource() throws Exception
    {
        final String url = "/download/batch/" + TEST_MODULE_COMPLETE_KEY + "/all.css";
        final String ieResourceName = "master-ie.css";
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors(ieResourceName, "master.css");

        final Plugin mockPlugin = mock(Plugin.class);
        final ModuleDescriptor mockModuleDescriptor = mock(ModuleDescriptor.class);
        when(mockModuleDescriptor.getPluginKey()).thenReturn(TEST_PLUGIN_KEY);
        when(mockModuleDescriptor.getCompleteKey()).thenReturn(TEST_MODULE_COMPLETE_KEY);
        when(mockModuleDescriptor.getResourceDescriptors()).thenReturn(resourceDescriptors);
        when(mockModuleDescriptor.getResourceLocation("download", ieResourceName)).thenReturn(
            new ResourceLocation("", ieResourceName, "download", "text/css", "", Collections.<String, String> emptyMap()));

        when(mockPluginAccessor.isPluginModuleEnabled(TEST_MODULE_COMPLETE_KEY)).thenReturn(Boolean.TRUE);
        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn(mockModuleDescriptor);
        when(mockPluginAccessor.getPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn(mockModuleDescriptor);
        when(mockPluginAccessor.getPlugin(TEST_PLUGIN_KEY)).thenReturn(mockPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, params);

        assertTrue(resource instanceof BatchPluginResource);
    }

    public void testGetDownloadableBatchResourceWithConditionalComments() throws Exception
    {
        final String url = "/download/batch/" + TEST_MODULE_COMPLETE_KEY + "/all.css";
        final String ieResourceName = "master-conditional.css";
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("conditionalComment", "IE");

        final List<ResourceDescriptor> resourceDescriptors = asList(TestUtils.createResourceDescriptor(ieResourceName, new HashMap<String, String>(
            params)), TestUtils.createResourceDescriptor(ieResourceName));

        final Plugin mockPlugin = mock(Plugin.class);
        final ModuleDescriptor mockModuleDescriptor = mock(ModuleDescriptor.class);
        when(mockModuleDescriptor.getPluginKey()).thenReturn(TEST_PLUGIN_KEY);
        when(mockModuleDescriptor.getCompleteKey()).thenReturn(TEST_MODULE_COMPLETE_KEY);
        when(mockModuleDescriptor.getResourceDescriptors()).thenReturn(resourceDescriptors);
        when(mockModuleDescriptor.getResourceLocation("download", ieResourceName)).thenReturn(
            new ResourceLocation("", ieResourceName, "download", "text/css", "", Collections.<String, String> emptyMap()));

        when(mockPluginAccessor.isPluginModuleEnabled(TEST_MODULE_COMPLETE_KEY)).thenReturn(Boolean.TRUE);
        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn(mockModuleDescriptor);
        when(mockPluginAccessor.getPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn(mockModuleDescriptor);
        when(mockPluginAccessor.getPlugin(TEST_PLUGIN_KEY)).thenReturn(mockPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, params);

        assertTrue(resource instanceof BatchPluginResource);
    }

    public void testGetDownloadableBatchResourceWhenModuleIsUnkown() throws Exception
    {
        final String url = "/download/batch/" + TEST_MODULE_COMPLETE_KEY + "invalid.stuff" + "/all.css";
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");

        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY + "invalid.stuff")).thenReturn(null);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, params);
        assertNull(resource);
    }

    public void testGetDownloadableBatchResourceFallbacksToSingle() throws Exception
    {
        final String resourceName = "images/foo.png";
        final String url = "/download/batch/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;

        final Plugin mockPlugin = mock(Plugin.class);
        final ModuleDescriptor mockModuleDescriptor = mock(ModuleDescriptor.class);
        when(mockModuleDescriptor.getPluginKey()).thenReturn(TEST_PLUGIN_KEY);
        when(mockModuleDescriptor.getCompleteKey()).thenReturn(TEST_MODULE_COMPLETE_KEY);
        when(mockModuleDescriptor.getResourceLocation("download", resourceName)).thenReturn(
            new ResourceLocation("", resourceName, "download", "text/css", "", Collections.<String, String> emptyMap()));

        when(mockPluginAccessor.isPluginModuleEnabled(TEST_MODULE_COMPLETE_KEY)).thenReturn(Boolean.TRUE);
        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn(mockModuleDescriptor);
        when(mockPluginAccessor.getPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn(mockModuleDescriptor);
        when(mockPluginAccessor.getPlugin(TEST_PLUGIN_KEY)).thenReturn(mockPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String> emptyMap());

        assertTrue(resource instanceof DownloadableClasspathResource);
    }

    public void testGetDownloadableSuperBatchResource() throws Exception
    {
        final String url = "/download/superbatch/css/batch.css";

        final Plugin testPlugin = TestUtils.createTestPlugin(TEST_PLUGIN_KEY, "1");
        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("atlassian.css", "master.css");

        final WebResourceModuleDescriptor webModuleDescriptor = TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY, testPlugin,
            resourceDescriptors);

        when(mockWebResourceIntegration.getSuperBatchVersion()).thenReturn("1.0");
        when(mockBatchingConfiguration.isSuperBatchingEnabled()).thenReturn(true);
        when(mockBatchingConfiguration.getSuperBatchModuleCompleteKeys()).thenReturn(Arrays.asList(TEST_MODULE_COMPLETE_KEY));

        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn((ModuleDescriptor) webModuleDescriptor);
        when(mockPluginAccessor.getPlugin(TEST_PLUGIN_KEY)).thenReturn(testPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String> emptyMap());
        assertTrue(resource instanceof SuperBatchPluginResource);

        final SuperBatchPluginResource superBatchPluginResource = (SuperBatchPluginResource) resource;
        assertFalse(superBatchPluginResource.isEmpty());
    }

    public void testGetDownloadableSuperBatchSubResource() throws Exception
    {
        final String url = "/download/superbatch/css/images/foo.png";
        final String cssResourcesXml = "<resource name=\"css/\" type=\"download\" location=\"css/images/\" />";

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("atlassian.css", "master.css");
        resourceDescriptors.add(new ResourceDescriptor(DocumentHelper.parseText(cssResourcesXml).getRootElement()));

        final Plugin testPlugin = TestUtils.createTestPlugin(TEST_PLUGIN_KEY, "1");
        final WebResourceModuleDescriptor webModuleDescriptor = TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY, testPlugin,
            resourceDescriptors);

        when(mockWebResourceIntegration.getSuperBatchVersion()).thenReturn("1.0");
        when(mockBatchingConfiguration.isSuperBatchingEnabled()).thenReturn(true);
        when(mockBatchingConfiguration.getSuperBatchModuleCompleteKeys()).thenReturn(Arrays.asList(TEST_MODULE_COMPLETE_KEY));

        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn((ModuleDescriptor) webModuleDescriptor);
        when(mockPluginAccessor.getPlugin(TEST_PLUGIN_KEY)).thenReturn(testPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String> emptyMap());
        assertTrue(resource instanceof BatchSubResource);
        assertFalse(((BatchSubResource) resource).isEmpty());
    }

    public void testGetDownloadableContextBatchSubResource() throws Exception
    {
        final String url = "/download/contextbatch/css/contexta,contextb/images/foo.png";
        final String cssResourcesXml = "<resource name=\"css/\" type=\"download\" location=\"css/images/\" />";

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("atlassian.css", "master.css");
        resourceDescriptors.add(new ResourceDescriptor(DocumentHelper.parseText(cssResourcesXml).getRootElement()));

        final String context = "contexta";
        final Set<String> contexts = new HashSet<String>();
        contexts.add(context);

        final Plugin testPlugin = TestUtils.createTestPlugin(TEST_PLUGIN_KEY, "1");
        final WebResourceModuleDescriptor webModuleDescriptor = TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY, testPlugin,
            resourceDescriptors, Collections.<String> emptyList(), contexts);

        when(mockPluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class)).thenReturn(Arrays.asList(webModuleDescriptor));
        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn((ModuleDescriptor) webModuleDescriptor);
        when(mockPluginAccessor.getPlugin(TEST_PLUGIN_KEY)).thenReturn(testPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String> emptyMap());
        assertTrue(resource instanceof BatchSubResource);
        assertFalse(((BatchSubResource) resource).isEmpty());
    }

    public void testGetDownloadableContextBatchResource() throws Exception
    {
        final Set<String> contexts = new HashSet<String>();
        final String context = "contextA";
        contexts.add(context);
        contexts.add("contextB");
        final String url = "/download/contextbatch/css/contextA/batch.css";
        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("atlassian.css", "master.css");

        final Plugin testPlugin = TestUtils.createTestPlugin(TEST_PLUGIN_KEY, "1");
        final WebResourceModuleDescriptor webModuleDescriptor = TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY, testPlugin,
            resourceDescriptors, Collections.<String> emptyList(), contexts);

        when(mockPluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class)).thenReturn(Arrays.asList(webModuleDescriptor));
        when(mockPluginAccessor.getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY)).thenReturn((ModuleDescriptor) webModuleDescriptor);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String> emptyMap());
        assertTrue(resource instanceof ContextBatchPluginResource);

        final ContextBatchPluginResource contextBatchPluginResource = (ContextBatchPluginResource) resource;
        assertEquals(1, size(contextBatchPluginResource.getContexts()));
        assertEquals(context, get(contextBatchPluginResource.getContexts(), 0));
        verify(mockPluginAccessor, times(3)).getEnabledPluginModule(TEST_MODULE_COMPLETE_KEY);
    }

    public void testGetDownloadableMergedContextBatchResource() throws Exception
    {
        final Set<String> contexts1 = new HashSet<String>();
        final String context1 = "contextA";
        contexts1.add(context1);

        final Set<String> contexts2 = new HashSet<String>();
        final String context2 = "contextB";
        contexts2.add(context2);
        final String url = "/download/contextbatch/css/contextA,contextB/batch.css";

        final Plugin testPlugin = TestUtils.createTestPlugin(TEST_PLUGIN_KEY, "1");

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("atlassian.css", "master.css");
        final String completeKey1 = TEST_PLUGIN_KEY + ":" + "a-resources";
        final WebResourceModuleDescriptor webModuleDescriptor1 = TestUtils.createWebResourceModuleDescriptor(completeKey1, testPlugin,
            resourceDescriptors1, Collections.<String> emptyList(), contexts1);

        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("test.css", "default.css");
        final String completeKey2 = TEST_PLUGIN_KEY + ":" + "b-resources";
        final WebResourceModuleDescriptor webModuleDescriptor2 = TestUtils.createWebResourceModuleDescriptor(completeKey2, testPlugin,
            resourceDescriptors2, Collections.<String> emptyList(), contexts2);

        when(mockPluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class)).thenReturn(
            Arrays.asList(webModuleDescriptor1, webModuleDescriptor2));

        when(mockPluginAccessor.getEnabledPluginModule(completeKey1)).thenReturn((ModuleDescriptor) webModuleDescriptor1);
        when(mockPluginAccessor.getEnabledPluginModule(completeKey2)).thenReturn((ModuleDescriptor) webModuleDescriptor2);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String> emptyMap());
        assertTrue(resource instanceof ContextBatchPluginResource);

        final ContextBatchPluginResource contextBatchPluginResource = (ContextBatchPluginResource) resource;
        assertEquals(2, size(contextBatchPluginResource.getContexts()));
        assertEquals(context1, get(contextBatchPluginResource.getContexts(), 0));
        assertEquals(context2, get(contextBatchPluginResource.getContexts(), 1));

        verify(mockPluginAccessor, times(3)).getEnabledPluginModule(completeKey1);
        verify(mockPluginAccessor, times(3)).getEnabledPluginModule(completeKey2);
    }

    public void testGetDownloadableMergedContextBatchResourceWithOverlap() throws Exception
    {
        final Set<String> contexts1 = new HashSet<String>();
        final String context1 = "contextA";
        contexts1.add(context1);

        final Set<String> contexts2 = new HashSet<String>();
        final String context2 = "contextB";
        contexts2.add(context2);
        final String url = "/download/contextbatch/css/contextA,contextB/batch.css";

        final Plugin testPlugin = TestUtils.createTestPlugin(TEST_PLUGIN_KEY, "1");

        final List<ResourceDescriptor> parentResourceDescriptors = TestUtils.createResourceDescriptors("parent.css");
        final String parentKey = TEST_PLUGIN_KEY + ":" + "parent-resources";
        final WebResourceModuleDescriptor parentWebModuleDescriptor = TestUtils.createWebResourceModuleDescriptor(parentKey, testPlugin,
            parentResourceDescriptors);

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("atlassian.css", "master.css");
        final String completeKey1 = TEST_PLUGIN_KEY + ":" + "a-resources";
        final WebResourceModuleDescriptor webModuleDescriptor1 = TestUtils.createWebResourceModuleDescriptor(completeKey1, testPlugin,
            resourceDescriptors1, Arrays.asList(parentKey), contexts1);

        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("test.css", "default.css");
        final String completeKey2 = TEST_PLUGIN_KEY + ":" + "b-resources";
        final WebResourceModuleDescriptor webModuleDescriptor2 = TestUtils.createWebResourceModuleDescriptor(completeKey2, testPlugin,
            resourceDescriptors2, Arrays.asList(parentKey), contexts2);

        when(mockPluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class)).thenReturn(
            Arrays.asList(parentWebModuleDescriptor, webModuleDescriptor1, webModuleDescriptor2));

        when(mockPluginAccessor.getEnabledPluginModule(parentKey)).thenReturn((ModuleDescriptor) parentWebModuleDescriptor);
        when(mockPluginAccessor.getEnabledPluginModule(completeKey1)).thenReturn((ModuleDescriptor) webModuleDescriptor1);
        when(mockPluginAccessor.getEnabledPluginModule(completeKey2)).thenReturn((ModuleDescriptor) webModuleDescriptor2);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String> emptyMap());
        assertTrue(resource instanceof ContextBatchPluginResource);

        final ContextBatchPluginResource contextBatchPluginResource = (ContextBatchPluginResource) resource;
        assertEquals(2, size(contextBatchPluginResource.getContexts()));
        assertEquals(context1, get(contextBatchPluginResource.getContexts(), 0));
        assertEquals(context2, get(contextBatchPluginResource.getContexts(), 1));

        verify(mockPluginAccessor, times(3)).getEnabledPluginModule(completeKey1);
        verify(mockPluginAccessor, times(3)).getEnabledPluginModule(completeKey2);

        // TODO - BN 2.9.0 - work out a better way to ensure that the parent isn't included twice.
        // 2 for dependency resolution
        // 1 for resource descriptor download
        verify(mockPluginAccessor, times(3)).getEnabledPluginModule(parentKey);
    }

    public void testSplitLastPathPart()
    {
        final String[] parts = pluginResourceLocator.splitLastPathPart("http://localhost:8080/confluence/download/foo/bar/baz");
        assertEquals(2, parts.length);
        assertEquals("http://localhost:8080/confluence/download/foo/bar/", parts[0]);
        assertEquals("baz", parts[1]);

        final String[] anotherParts = pluginResourceLocator.splitLastPathPart(parts[0]);
        assertEquals(2, anotherParts.length);
        assertEquals("http://localhost:8080/confluence/download/foo/", anotherParts[0]);
        assertEquals("bar/", anotherParts[1]);

        assertNull(pluginResourceLocator.splitLastPathPart("noslashes"));
    }
}