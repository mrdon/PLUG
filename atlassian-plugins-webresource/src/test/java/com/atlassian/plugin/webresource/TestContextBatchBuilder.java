package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import junit.framework.TestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;

public class TestContextBatchBuilder extends TestCase
{
    public static final String PLUGIN_DESCRIPTOR = "test.atlassian:";
    @Mock
    private WebResourceIntegration mockWebResourceIntegration;
    private PluginResourceLocator pluginResourceLocator;
    @Mock
    private ResourceBatchingConfiguration mockBatchingConfiguration;
    @Mock
    private ResourceDependencyResolver mockDependencyResolver;
    @Mock
    private PluginAccessor mockPluginAccessor;

    private Plugin plugin;
    List<WebResourceModuleDescriptor> moduleDescriptors = new ArrayList<WebResourceModuleDescriptor>();
    private ContextBatchBuilder builder;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        plugin = TestUtils.createTestPlugin();
        when(mockWebResourceIntegration.getPluginAccessor()).thenReturn(mockPluginAccessor);
        when(mockPluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class)).thenReturn(moduleDescriptors);
        pluginResourceLocator = new PluginResourceLocatorImpl(mockWebResourceIntegration, null);

        builder = new ContextBatchBuilder(mockWebResourceIntegration, pluginResourceLocator, mockBatchingConfiguration, mockDependencyResolver);
    }

    @Override
    public void tearDown() throws Exception
    {
        mockWebResourceIntegration = null;
        pluginResourceLocator = null;
        mockBatchingConfiguration = null;
        mockDependencyResolver = null;
        mockPluginAccessor = null;

        plugin = null;
        moduleDescriptors = null;
        builder = null;

        super.tearDown();
    }

    public void testNoOverlapAndNoDependencies() throws Exception
    {
        String context1 = "xmen";
        String context2 = "brotherhood";
        Set<String> contexts = new HashSet<String>();
        contexts.add(context1);
        contexts.add(context2);

        final String moduleKey1 = "xavier-resources";
        final String moduleKey2 = "magneto-resources";
        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("professorx.js", "professorx.css", "wolverine.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("magneto.js", "magneto.css", "sabretooth.css");

        addModuleDescriptor(moduleKey1, resourceDescriptors1, Arrays.asList(context1));
        addModuleDescriptor(moduleKey2, resourceDescriptors2, Arrays.asList(context2));

        List<PluginResource> resources = builder.build(contexts);

        assertEquals(4, resources.size());
        assertEquals("/download/superbatch/css/xmen", resources.get(0).getUrl());
        assertEquals("/download/superbatch/js/xmen", resources.get(1).getUrl());
        assertEquals("/download/superbatch/css/brotherhood", resources.get(2).getUrl());
        assertEquals("/download/superbatch/js/brotherhood", resources.get(3).getUrl());
    }

    public void testOverlappingAndNoDependencies() throws Exception
    {
        String context1 = "xmen";
        String context2 = "brotherhood";
        Set<String> contexts = new HashSet<String>();
        contexts.add(context1);
        contexts.add(context2);

        final String moduleKey1 = "xavier-resources";
        final String moduleKey2 = "magneto-resources";
        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("professorx.js", "professorx.css", "wolverine.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("magneto.js", "magneto.css", "sabretooth.css");

        addModuleDescriptor(moduleKey1, resourceDescriptors1, contexts);
        addModuleDescriptor(moduleKey2, resourceDescriptors2, contexts);

        List<PluginResource> resources = builder.build(contexts);

        assertEquals(2, resources.size());
        assertEquals("/download/superbatch/css/xmen+brotherhood", resources.get(0).getUrl());
        assertEquals("/download/superbatch/js/xmen+brotherhood", resources.get(1).getUrl());
    }

    public void testDependenciesNoOverlap() throws Exception
    {
        String context1 = "xmen";
        String context2 = "brotherhood";
        Set<String> contexts = new HashSet<String>();
        contexts.add(context1);
        contexts.add(context2);

        final String moduleKey1 = "xavier-resources";
        final String moduleKey2 = "magneto-resources";
        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("professorx.js", "professorx.css", "wolverine.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("magneto.js", "magneto.css", "sabretooth.css");

        final String dependentModule1 = "new-mutants-resources";
        final List<ResourceDescriptor> dependentResourceDescriptors1 = TestUtils.createResourceDescriptors("karma.js", "cannonball.css", "wolfsbane.css");

        addModuleDescriptor(moduleKey1, resourceDescriptors1, Collections.<String>emptySet());
        addModuleDescriptor(dependentModule1, dependentResourceDescriptors1, Arrays.asList(moduleKey1), Arrays.asList(context1));
        addModuleDescriptor(moduleKey2, resourceDescriptors2, Arrays.asList(context2));

        List<PluginResource> resources = builder.build(contexts);

        assertEquals(4, resources.size());
        assertEquals("/download/superbatch/css/xmen", resources.get(0).getUrl());
        assertEquals("/download/superbatch/js/xmen", resources.get(1).getUrl());
        assertEquals("/download/superbatch/css/brotherhood", resources.get(2).getUrl());
        assertEquals("/download/superbatch/js/brotherhood", resources.get(3).getUrl());
    }

    private void addModuleDescriptor(String moduleKey, List<ResourceDescriptor> descriptors, Collection<String> contexts)
    {
        addModuleDescriptor(moduleKey, descriptors, new ArrayList<String>(), contexts);
    }

    private void addModuleDescriptor(String moduleKey, List<ResourceDescriptor> descriptors, List<String> dependencies, Collection<String> contexts)
    {
        String completeKey = PLUGIN_DESCRIPTOR + moduleKey;

        Set<String> contextSet = new HashSet<String>();
        contextSet.addAll(contexts);

        WebResourceModuleDescriptor moduleDescriptor = TestUtils.createWebResourceModuleDescriptor(completeKey,
                plugin, descriptors, Collections.<String>emptyList(), contextSet);

        moduleDescriptors.add(moduleDescriptor);

        LinkedHashSet<String> dependenciesSet = new LinkedHashSet<String>(dependencies);
        dependenciesSet.add(completeKey);

        when(mockPluginAccessor.getEnabledPluginModule(completeKey)).thenReturn((ModuleDescriptor) moduleDescriptor);
        when(mockDependencyResolver.getDependencies(completeKey, true)).thenReturn(dependenciesSet);
    }
}
