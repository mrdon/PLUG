package com.atlassian.plugin.webresource;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static org.mockito.Mockito.when;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceDescriptor;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class TestDefaultResourceDependencyResolver extends TestCase
{
    @Mock
    private WebResourceIntegration mockWebResourceIntegration;
    @Mock
    private PluginAccessor mockPluginAccessor;
    @Mock
    private ResourceBatchingConfiguration mockBatchingConfiguration;

    private ResourceDependencyResolver dependencyResolver;

    private Plugin testPlugin;
    private final List<String> superBatchKeys = new ArrayList<String>();
    private final List<WebResourceModuleDescriptor> moduleDescriptors = new ArrayList<WebResourceModuleDescriptor>();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        testPlugin = TestUtils.createTestPlugin();

        MockitoAnnotations.initMocks(this);
        when(mockWebResourceIntegration.getPluginAccessor()).thenReturn(mockPluginAccessor);
        when(mockWebResourceIntegration.getSuperBatchVersion()).thenReturn("1");
        when(mockPluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class)).thenReturn(moduleDescriptors);
        when(mockBatchingConfiguration.isSuperBatchingEnabled()).thenReturn(true);
        when(mockBatchingConfiguration.getSuperBatchModuleCompleteKeys()).thenReturn(superBatchKeys);

        dependencyResolver = new DefaultResourceDependencyResolver(mockWebResourceIntegration, mockBatchingConfiguration);
    }

    @Override
    protected void tearDown() throws Exception
    {
        dependencyResolver = null;
        mockWebResourceIntegration = null;
        mockPluginAccessor = null;
        mockBatchingConfiguration = null;

        testPlugin = null;

        super.tearDown();
    }

    public void testSuperBatchingNotEnabled()
    {
        when(mockBatchingConfiguration.isSuperBatchingEnabled()).thenReturn(true);
        when(mockBatchingConfiguration.getSuperBatchModuleCompleteKeys()).thenReturn(superBatchKeys);

        assertTrue(isEmpty(dependencyResolver.getSuperBatchDependencies()));
    }

    public void testGetSuperBatchDependenciesInOrder()
    {
        final String superBatchResource1 = "plugin.key:resource1";
        final String superBatchResource2 = "plugin.key:resource2";
        final String superBatchResource3 = "plugin.key:resource3";

        superBatchKeys.add(superBatchResource1);
        superBatchKeys.add(superBatchResource2);
        superBatchKeys.add(superBatchResource3);

        when(mockWebResourceIntegration.getSuperBatchVersion()).thenReturn("1.0");

        addModuleDescriptor(superBatchResource1);
        addModuleDescriptor(superBatchResource2);
        addModuleDescriptor(superBatchResource3);

        final Iterable<WebResourceModuleDescriptor> resources = dependencyResolver.getSuperBatchDependencies();
        assertNotNull(resources);
        assertOrder(resources, superBatchResource1, superBatchResource2, superBatchResource3);
    }

    public void testGetSuperBatchDependenciesWithCylicDependency()
    {
        final String superBatchResource1 = "plugin.key:resource1";
        final String superBatchResource2 = "plugin.key:resource2";

        superBatchKeys.add(superBatchResource1);
        superBatchKeys.add(superBatchResource2);

        when(mockWebResourceIntegration.getSuperBatchVersion()).thenReturn("1.0");

        addModuleDescriptor(superBatchResource1, Arrays.asList(superBatchResource2));
        addModuleDescriptor(superBatchResource2, Arrays.asList(superBatchResource1));

        final Iterable<WebResourceModuleDescriptor> resources = dependencyResolver.getSuperBatchDependencies();
        assertNotNull(resources);
        assertOrder(resources, superBatchResource2, superBatchResource1);
    }

    public void testGetSuperBatchDependenciesWithDependencies()
    {
        final String superBatchResource1 = "test.atlassian:super1";
        final String superBatchResource2 = "test.atlassian:super2";

        superBatchKeys.add(superBatchResource1);
        superBatchKeys.add(superBatchResource2);
        when(mockWebResourceIntegration.getSuperBatchVersion()).thenReturn("1.0");

        // dependcies
        final String resourceA = "test.atlassian:a";
        final String resourceB = "test.atlassian:b";
        final String resourceC = "test.atlassian:c";
        final String resourceD = "test.atlassian:d";

        // super batch 1 depends on A, B
        addModuleDescriptor(resourceA);
        addModuleDescriptor(resourceB);
        addModuleDescriptor(superBatchResource1, Arrays.asList(resourceA, resourceB));

        // super batch 2 depends on C
        addModuleDescriptor(resourceD);
        addModuleDescriptor(resourceC, Arrays.asList(resourceD));
        addModuleDescriptor(superBatchResource2, Collections.singletonList(resourceC));

        final Iterable<WebResourceModuleDescriptor> resources = dependencyResolver.getSuperBatchDependencies();
        assertNotNull(resources);
        assertOrder(resources, resourceA, resourceB, superBatchResource1, resourceD, resourceC, superBatchResource2);
    }

    public void testGetDependenciesExcludesSuperBatch()
    {
        final String superBatchResource1 = "test.atlassian:super1";
        final String superBatchResource2 = "test.atlassian:super2";
        final String moduleKey = "test.atlassian:foo";

        superBatchKeys.add(superBatchResource1);
        superBatchKeys.add(superBatchResource2);
        when(mockWebResourceIntegration.getSuperBatchVersion()).thenReturn("1.0");

        // module depends on super batch 1
        addModuleDescriptor(moduleKey, Arrays.asList(superBatchResource1));
        addModuleDescriptor(superBatchResource1);
        addModuleDescriptor(superBatchResource2);

        final Iterable<WebResourceModuleDescriptor> resources = dependencyResolver.getDependencies(moduleKey, true);
        assertNotNull(resources);
        assertOrder(resources, moduleKey);
    }

    public void testGetDependenciesIncludesSuperBatch()
    {
        final String superBatchResource1 = "test.atlassian:super1";
        final String superBatchResource2 = "test.atlassian:super2";
        final String moduleKey = "test.atlassian:foo";

        superBatchKeys.add(superBatchResource1);
        superBatchKeys.add(superBatchResource2);
        when(mockWebResourceIntegration.getSuperBatchVersion()).thenReturn("1.0");

        // module depends on super batch 1
        addModuleDescriptor(moduleKey, Arrays.asList(superBatchResource1));
        addModuleDescriptor(superBatchResource1);
        addModuleDescriptor(superBatchResource2);

        final Iterable<WebResourceModuleDescriptor> resources = dependencyResolver.getDependencies(moduleKey, false);
        assertNotNull(resources);
        assertOrder(resources, superBatchResource1, moduleKey);
    }

    public void testGetDependenciesInContext()
    {
        final String moduleKey1 = "test.atlassian:foo";
        final String moduleKey2 = "test.atlassian:bar";

        final String context1 = "connie";
        final Set<String> contexts1 = new HashSet<String>(Arrays.asList(context1));
        final String context2 = "jira";
        final Set<String> contexts2 = new HashSet<String>(Arrays.asList(context2));

        addModuleDescriptor(moduleKey1, Collections.<String> emptyList(), contexts1);
        addModuleDescriptor(moduleKey2, Collections.<String> emptyList(), contexts2);

        Iterable<WebResourceModuleDescriptor> resources = dependencyResolver.getDependenciesInContext(context1);
        assertOrder(resources, moduleKey1);

        resources = dependencyResolver.getDependenciesInContext(context2);
        assertOrder(resources, moduleKey2);
    }

    public void testGetDependenciesInContextWithMultipleEntries()
    {
        final String moduleKey1 = "test.atlassian:foo";
        final String moduleKey2 = "test.atlassian:bar";

        final String context = "connie";
        final Set<String> contexts = new HashSet<String>(Arrays.asList(context));

        addModuleDescriptor(moduleKey1, Collections.<String> emptyList(), contexts);
        addModuleDescriptor(moduleKey2, Collections.<String> emptyList(), contexts);

        final Iterable<WebResourceModuleDescriptor> resources = dependencyResolver.getDependenciesInContext(context);
        assertOrder(resources, moduleKey1, moduleKey2);
    }

    public void testGetDependenciesInContextWithSharedDependenciesInDifferentContexts()
    {
        final String moduleKey1 = "test.atlassian:foo";
        final String moduleKey2 = "test.atlassian:bar";
        final String sharedModuleKey = "test.atlassian:shared";

        final String context1 = "connie";
        final Set<String> contexts1 = new HashSet<String>(Arrays.asList(context1));
        final String context2 = "jira";
        final Set<String> contexts2 = new HashSet<String>(Arrays.asList(context2));

        addModuleDescriptor(moduleKey1, Arrays.asList(sharedModuleKey), contexts1);
        addModuleDescriptor(moduleKey2, Arrays.asList(sharedModuleKey), contexts2);
        // Even if the parent is added last, it should appear first in the list.
        addModuleDescriptor(sharedModuleKey);

        Iterable<WebResourceModuleDescriptor> resources = dependencyResolver.getDependenciesInContext(context1);
        assertOrder(resources, sharedModuleKey, moduleKey1);

        resources = dependencyResolver.getDependenciesInContext(context2);
        assertOrder(resources, sharedModuleKey, moduleKey2);
    }

    public void testGetDependenciesInContextWithSharedDependenciesInTheSameContext()
    {
        final String moduleKey1 = "test.atlassian:foo";
        final String moduleKey2 = "test.atlassian:bar";
        final String sharedModuleKey = "test.atlassian:shared";

        final String context = "connie";
        final Set<String> contexts = new HashSet<String>(Arrays.asList(context));

        addModuleDescriptor(moduleKey1, Arrays.asList(sharedModuleKey), contexts);
        addModuleDescriptor(moduleKey2, Arrays.asList(sharedModuleKey), contexts);
        // Even if the parent is added last, it should appear first in the list.
        addModuleDescriptor(sharedModuleKey);

        final Iterable<WebResourceModuleDescriptor> resources = dependencyResolver.getDependenciesInContext(context);
        assertOrder(resources, sharedModuleKey, moduleKey1, moduleKey2);
    }

    public void testGetDependenciesInContextWithDuplicateResources()
    {
        final String moduleKey1 = "test.atlassian:foo";
        final String parentModuleKey = "test.atlassian:parent";

        final String context1 = "connie";
        final Set<String> contexts1 = new HashSet<String>(Arrays.asList(context1));

        addModuleDescriptor(parentModuleKey, Collections.<String> emptyList(), contexts1);
        addModuleDescriptor(moduleKey1, Arrays.asList(parentModuleKey), contexts1);

        final Iterable<WebResourceModuleDescriptor> resources = dependencyResolver.getDependenciesInContext(context1);
        assertOrder(resources, parentModuleKey, moduleKey1);
    }

    public void testSkippedConditions() throws ClassNotFoundException
    {
        testPlugin = TestUtils.createTestPlugin("test.atlassian", "1", AlwaysTrueCondition.class, AlwaysFalseCondition.class);
        final String moduleKey1 = "test.atlassian:foo";
        final String parentModuleKey = "test.atlassian:parent";
        final String skippedParentModuleKey = "test.atlassian:skipped-parent";
        final String skippedModuleKey = "test.atlassian:skipped";

        final String context1 = "connie";
        final Set<String> contexts1 = new HashSet<String>(Arrays.asList(context1));

        addModuleDescriptor(parentModuleKey, Collections.<String> emptyList(), contexts1);
        addModuleDescriptor(moduleKey1, Arrays.asList(parentModuleKey), contexts1);
        addModuleDescriptor(skippedParentModuleKey, Collections.<String> emptyList());

        final WebResourceModuleDescriptor skippedModuleDescriptor = new WebResourceModuleDescriptorBuilder(testPlugin, "skipped").setCondition(
            AlwaysTrueCondition.class).addDescriptor("skipped.js").addDependency(skippedParentModuleKey).addContext(context1).build();
        addModuleDescriptor(skippedModuleDescriptor);

        final Set<String> skippedResources = new HashSet<String>();
        final Iterable<WebResourceModuleDescriptor> resources = dependencyResolver.getDependenciesInContext(context1, skippedResources);
        assertOrder(resources, parentModuleKey, moduleKey1);

        // The parent shouldn't be included
        assertEquals(1, skippedResources.size());
        assertEquals(skippedModuleKey, skippedResources.toArray()[0]);
    }

    private void addModuleDescriptor(final String moduleKey)
    {
        addModuleDescriptor(moduleKey, Collections.<String> emptyList());
    }

    private void addModuleDescriptor(final String moduleKey, final List<String> dependencies)
    {
        addModuleDescriptor(moduleKey, dependencies, Collections.<String> emptySet());
    }

    private void addModuleDescriptor(final String moduleKey, final List<String> dependencies, final Set<String> contexts)
    {
        final WebResourceModuleDescriptor webResourceModuleDescriptor = TestUtils.createWebResourceModuleDescriptor(moduleKey, testPlugin,
            Collections.<ResourceDescriptor> emptyList(), dependencies, contexts);

        addModuleDescriptor(webResourceModuleDescriptor);
    }

    private void addModuleDescriptor(final WebResourceModuleDescriptor webResourceModuleDescriptor)
    {
        moduleDescriptors.add(webResourceModuleDescriptor);

        when(mockPluginAccessor.getEnabledPluginModule(webResourceModuleDescriptor.getCompleteKey())).thenReturn(
            (ModuleDescriptor) webResourceModuleDescriptor);
    }

    private void assertOrder(final Iterable<WebResourceModuleDescriptor> resources, final String... expectedResources)
    {
        assertEquals(size(resources), expectedResources.length);

        int i = 0;
        for (final WebResourceModuleDescriptor resource : resources)
        {
            assertEquals(expectedResources[i], resource.getCompleteKey());
            i++;
        }
    }
}
