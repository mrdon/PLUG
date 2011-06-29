package com.atlassian.plugin.webresource;

import com.atlassian.plugin.elements.ResourceDescriptor;
import junit.framework.TestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;

public class TestContextBatchBuilder extends TestCase
{
    public static final String PLUGIN_KEY = "test.atlassian:";

    @Mock
    private ResourceDependencyResolver mockDependencyResolver;
    @Mock
    private PluginResourceLocator mockPluginResourceLocator;

    private ContextBatchBuilder builder;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        builder = new ContextBatchBuilder(mockPluginResourceLocator, mockDependencyResolver);
    }

    @Override
    public void tearDown() throws Exception
    {
        mockPluginResourceLocator = null;
        mockDependencyResolver = null;

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
        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("professorx.js", "professorx.css", "cyclops.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("magneto.js", "magneto.css", "sabretooth.css");

        addModuleDescriptor(moduleKey1, resourceDescriptors1);
        addModuleDescriptor(moduleKey2, resourceDescriptors2);
        addContext(context1, Arrays.asList(moduleKey1));
        addContext(context2, Arrays.asList(moduleKey2));

        List<PluginResource> resources = builder.build(contexts);

        assertEquals(4, resources.size());
        assertEquals("/download/contextbatch/js/xmen.js", resources.get(0).getUrl());
        assertEquals("/download/contextbatch/css/xmen.css", resources.get(1).getUrl());
        assertEquals("/download/contextbatch/js/brotherhood.js", resources.get(2).getUrl());
        assertEquals("/download/contextbatch/css/brotherhood.css", resources.get(3).getUrl());

        assertEquals(2, builder.getAllIncludedResources().size());
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
        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("professorx.js", "professorx.css", "cyclops.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("magneto.js", "magneto.css", "sabretooth.css");

        addModuleDescriptor(moduleKey1, resourceDescriptors1);
        addModuleDescriptor(moduleKey2, resourceDescriptors2);
        addContext(context1, Arrays.asList(moduleKey1, moduleKey2));
        addContext(context2, Arrays.asList(moduleKey1, moduleKey2));

        List<PluginResource> resources = builder.build(contexts);

        assertEquals(2, resources.size());
        assertEquals("/download/contextbatch/js/xmen%252Bbrotherhood.js", resources.get(0).getUrl());
        assertEquals("/download/contextbatch/css/xmen%252Bbrotherhood.css", resources.get(1).getUrl());

        assertEquals(2, builder.getAllIncludedResources().size());
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
        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("professorx.js", "professorx.css", "cyclops.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("magneto.js", "magneto.css", "sabretooth.css");

        final String dependentModule1 = "students-resources";
        final List<ResourceDescriptor> dependentResourceDescriptors1 = TestUtils.createResourceDescriptors("iceman.js", "iceman.css", "rogue.css");
        final String dependentModule2 = "evil-students-resources";
        final List<ResourceDescriptor> dependentResourceDescriptors2 = TestUtils.createResourceDescriptors("pyro.css");

        addModuleDescriptor(moduleKey1, resourceDescriptors1);
        addModuleDescriptor(dependentModule1, dependentResourceDescriptors1);
        addContext(context1, Arrays.asList(moduleKey1, dependentModule1));

        addModuleDescriptor(moduleKey2, resourceDescriptors2);
        addModuleDescriptor(dependentModule2, dependentResourceDescriptors2);
        addContext(context2, Arrays.asList(moduleKey2, dependentModule2));

        List<PluginResource> resources = builder.build(contexts);

        assertEquals(4, resources.size());
        assertEquals("/download/contextbatch/js/xmen.js", resources.get(0).getUrl());
        assertEquals("/download/contextbatch/css/xmen.css", resources.get(1).getUrl());
        assertEquals("/download/contextbatch/js/brotherhood.js", resources.get(2).getUrl());
        assertEquals("/download/contextbatch/css/brotherhood.css", resources.get(3).getUrl());

        assertEquals(4, builder.getAllIncludedResources().size());
    }

    public void testOverlappingDependencies() throws Exception
    {
        String context1 = "xmen";
        String context2 = "government";
        String context3 = "brotherhood";
        Set<String> contexts = new HashSet<String>();
        contexts.add(context1);
        contexts.add(context2);

        final String moduleKey1 = "xavier-resources";
        final String moduleKey2 = "magneto-resources";
        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("professorx.js", "professorx.css", "cyclops.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("magneto.js", "magneto.css", "sabretooth.css");

        final String dependentModule1 = "new-mutants-resources";
        final List<ResourceDescriptor> dependentResourceDescriptors1 = TestUtils.createResourceDescriptors("iceman.js", "iceman.css", "rogue.css");

        final String dependentModule2 = "government-resources";
        final List<ResourceDescriptor> dependentResourceDescriptors2 = TestUtils.createResourceDescriptors("beast.js", "beast.js", "deathstrike.css");

        addModuleDescriptor(moduleKey1, resourceDescriptors1);
        addModuleDescriptor(dependentModule1, dependentResourceDescriptors1);
        addContext(context1, Arrays.asList(moduleKey1, dependentModule1));
        addModuleDescriptor(dependentModule2, dependentResourceDescriptors2);
        addContext(context2, Arrays.asList(moduleKey1, dependentModule2));

        // This shouldn't be added at all
        addModuleDescriptor(moduleKey2, resourceDescriptors2);
        addContext(context3, Arrays.asList(moduleKey2));

        List<PluginResource> resources = builder.build(contexts);

        assertEquals(2, resources.size());
        assertEquals("/download/contextbatch/js/xmen%252Bgovernment.js", resources.get(0).getUrl());
        assertEquals("/download/contextbatch/css/xmen%252Bgovernment.css", resources.get(1).getUrl());

        assertEquals(3, builder.getAllIncludedResources().size());
    }

    public void testMultipleOverlappingContexts() throws Exception
    {
        String context1 = "xmen";
        String context2 = "brotherhood";
        String context3 = "rogue";
        String context4 = "normals";

        Set<String> contexts = new HashSet<String>();
        contexts.add(context1);
        contexts.add(context2);
        contexts.add(context3);
        contexts.add(context4);

        final String moduleKey1 = "xavier-resources";
        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("professorx.js", "professorx.css", "beast.css");

        final String moduleKey2 = "magneto-resources";
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("magneto.js", "magneto.css", "sabretooth.css");

        final String moduleKey3 = "rogue-resources";
        final List<ResourceDescriptor> resourceDescriptors3 = TestUtils.createResourceDescriptors("pyro.js", "phoenix.css", "mystique.css");

        final String moduleKey4 = "normal-resources";
        final List<ResourceDescriptor> resourceDescriptors4 = TestUtils.createResourceDescriptors("stryker.js", "stryker.css");

        addModuleDescriptor(moduleKey1, resourceDescriptors1);
        addModuleDescriptor(moduleKey2, resourceDescriptors2);
        addModuleDescriptor(moduleKey3, resourceDescriptors3);
        addModuleDescriptor(moduleKey4, resourceDescriptors4);

        addContext(context1, Arrays.asList(moduleKey1));
        addContext(context2, Arrays.asList(moduleKey2));
        addContext(context3, Arrays.asList(moduleKey1, moduleKey2, moduleKey3));

        addContext(context4, Arrays.asList(moduleKey4));

        List<PluginResource> resources = builder.build(contexts);

        assertEquals(4, resources.size());
        assertEquals("/download/contextbatch/js/xmen%252Bbrotherhood%252Brogue.js", resources.get(0).getUrl());
        assertEquals("/download/contextbatch/css/xmen%252Bbrotherhood%252Brogue.css", resources.get(1).getUrl());
        assertEquals("/download/contextbatch/js/normals.js", resources.get(2).getUrl());
        assertEquals("/download/contextbatch/css/normals.css", resources.get(3).getUrl());

        assertEquals(4, builder.getAllIncludedResources().size());
    }

    public void testContextParams() throws Exception
    {
        String context1 = "xmen";
        String context2 = "brotherhood";
        Set<String> contexts = new HashSet<String>();
        contexts.add(context1);
        contexts.add(context2);

        final String moduleKey1 = "xavier-resources";
        final String moduleKey2 = "magneto-resources";
        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("professorx.js", "cyclops.css", "storm-ie.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("magneto.js", "sabretooth.css", "mystigue-ie.css");

        addModuleDescriptor(moduleKey1, resourceDescriptors1);
        addContext(context1, Arrays.asList(moduleKey1));
        addModuleDescriptor(moduleKey2, resourceDescriptors2);
        addContext(context2, Arrays.asList(moduleKey2));

        List<PluginResource> resources = builder.build(contexts);

        assertEquals(6, resources.size());
        assertEquals("/download/contextbatch/css/xmen.css?ieonly=true", resources.get(0).getUrl());
        assertEquals("/download/contextbatch/js/xmen.js", resources.get(1).getUrl());
        assertEquals("/download/contextbatch/css/xmen.css", resources.get(2).getUrl());
        assertEquals("/download/contextbatch/css/brotherhood.css?ieonly=true", resources.get(3).getUrl());
        assertEquals("/download/contextbatch/js/brotherhood.js", resources.get(4).getUrl());
        assertEquals("/download/contextbatch/css/brotherhood.css", resources.get(5).getUrl());

        assertEquals(2, builder.getAllIncludedResources().size());
    }

    public void testContextParamsInDependencies() throws Exception
    {
        String context1 = "xmen";
        String context2 = "brotherhood";
        String context3 = "rogue";
        Set<String> contexts = new HashSet<String>();
        contexts.add(context1);
        contexts.add(context2);
        contexts.add(context3);

        final String moduleKey1 = "xavier-resources";
        final String moduleKey2 = "magneto-resources";
        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("professorx.js", "professorx-ie.css", "cyclops.js");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("magneto.js", "magneto-ie.css", "sabretooth.css");

        final String dependentModule1 = "rogue-resources";
        final List<ResourceDescriptor> dependentResourceDescriptors1 = TestUtils.createResourceDescriptors("nightcrawler.js", "nightcrawler.css", "gambit.css");

        addModuleDescriptor(moduleKey1, resourceDescriptors1);
        addContext(context1, Arrays.asList(moduleKey1));
        addModuleDescriptor(dependentModule1, dependentResourceDescriptors1);
        addContext(context3, Arrays.asList(moduleKey1, dependentModule1));
        addModuleDescriptor(moduleKey2, resourceDescriptors2);
        addContext(context2, Arrays.asList(moduleKey2));

        List<PluginResource> resources = builder.build(contexts);

        // We currently batch all resource params even if there isn't any overlap in a particular context/param combination
        assertEquals(6, resources.size());
        assertEquals("/download/contextbatch/css/xmen%252Brogue.css?ieonly=true", resources.get(0).getUrl());
        assertEquals("/download/contextbatch/js/xmen%252Brogue.js", resources.get(1).getUrl());
        assertEquals("/download/contextbatch/css/xmen%252Brogue.css", resources.get(2).getUrl());
        assertEquals("/download/contextbatch/css/brotherhood.css?ieonly=true", resources.get(3).getUrl());
        assertEquals("/download/contextbatch/js/brotherhood.js", resources.get(4).getUrl());
        assertEquals("/download/contextbatch/css/brotherhood.css", resources.get(5).getUrl());

        assertEquals(3, builder.getAllIncludedResources().size());
    }

    private void addContext(String context, List<String> descriptors)
    {
        List<String> fullKeyDescriptors = new ArrayList<String>();

        for (String key : descriptors)
        {
            fullKeyDescriptors.add(PLUGIN_KEY + key);
        }

        when(mockDependencyResolver.getDependenciesInContext(context, Collections.<String>emptySet())).thenReturn(fullKeyDescriptors);
    }

    private void addModuleDescriptor(String moduleKey, List<ResourceDescriptor> descriptors)
    {
        String completeKey = PLUGIN_KEY + moduleKey;

        List<PluginResource> pluginResources = new ArrayList<PluginResource>();
        for (ResourceDescriptor descriptor : descriptors)
        {
            pluginResources.add(new SinglePluginResource(descriptor.getName(), completeKey, false, descriptor.getParameters()));
        }

        when(mockPluginResourceLocator.getPluginResources(completeKey)).thenReturn(pluginResources);
    }
}
