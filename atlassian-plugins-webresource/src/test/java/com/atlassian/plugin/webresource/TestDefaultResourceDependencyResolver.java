package com.atlassian.plugin.webresource;

import junit.framework.TestCase;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.Plugin;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.Arrays;

public class TestDefaultResourceDependencyResolver extends TestCase
{
    private Mock mockWebResourceIntegration;
    private Mock mockPluginAccessor;
    private ResourceDependencyResolver dependencyResolver;

    private Plugin testPlugin;
    private List<String> superBatchKeys = new ArrayList<String>();
    private ResourceBatchingConfiguration batchingConfiguration = new ResourceBatchingConfiguration() {

        public boolean isSuperBatchingEnabled()
        {
            return true;
        }

        public List<String> getSuperBatchModuleCompleteKeys()
        {
            return superBatchKeys;
        }
    };

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        testPlugin = TestUtils.createTestPlugin();

        mockPluginAccessor = new Mock(PluginAccessor.class);
        mockWebResourceIntegration = new Mock(WebResourceIntegration.class);
        mockWebResourceIntegration.matchAndReturn("getPluginAccessor", mockPluginAccessor.proxy());

        dependencyResolver = new DefaultResourceDependencyResolver((WebResourceIntegration) mockWebResourceIntegration.proxy(), batchingConfiguration);
    }

    @Override
    protected void tearDown() throws Exception
    {
        dependencyResolver = null;
        mockWebResourceIntegration = null;
        mockPluginAccessor = null;

        testPlugin = null;

        super.tearDown();
    }

    public void testSuperBatchingNotEnabled()
    {
        dependencyResolver = new DefaultResourceDependencyResolver((WebResourceIntegration) mockWebResourceIntegration.proxy(), new ResourceBatchingConfiguration() {

            public boolean isSuperBatchingEnabled()
            {
                return false;
            }

            public List<String> getSuperBatchModuleCompleteKeys()
            {
                return null;
            }
        });

        assertTrue(dependencyResolver.getSuperBatchDependencies().isEmpty());
    }

    public void testGetSuperBatchDependenciesInOrder()
    {
        String superBatchResource1 = "plugin.key:resource1";
        String superBatchResource2 = "plugin.key:resource2";
        String superBatchResource3 = "plugin.key:resource3";

        superBatchKeys.add(superBatchResource1);
        superBatchKeys.add(superBatchResource2);
        superBatchKeys.add(superBatchResource3);

        mockWebResourceIntegration.matchAndReturn("getSuperBatchVersion", "1.0");

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(superBatchResource1)),
            TestUtils.createWebResourceModuleDescriptor(superBatchResource1, testPlugin));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(superBatchResource2)),
            TestUtils.createWebResourceModuleDescriptor(superBatchResource2, testPlugin));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(superBatchResource3)),
            TestUtils.createWebResourceModuleDescriptor(superBatchResource3, testPlugin));

        LinkedHashSet<String> resources = dependencyResolver.getSuperBatchDependencies();
        assertNotNull(resources);
        assertOrder(resources, superBatchResource1, superBatchResource2, superBatchResource3);
    }

    public void testGetSuperBatchDependenciesWithCylicDependency()
    {
        String superBatchResource1 = "plugin.key:resource1";
        String superBatchResource2 = "plugin.key:resource2";

        superBatchKeys.add(superBatchResource1);
        superBatchKeys.add(superBatchResource2);

        mockWebResourceIntegration.matchAndReturn("getSuperBatchVersion", "1.0");

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(superBatchResource1)),
            TestUtils.createWebResourceModuleDescriptor(superBatchResource1, testPlugin, Collections.EMPTY_LIST, Arrays.asList(superBatchResource2)));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(superBatchResource2)),
            TestUtils.createWebResourceModuleDescriptor(superBatchResource2, testPlugin, Collections.EMPTY_LIST, Arrays.asList(superBatchResource1)));

        LinkedHashSet<String> resources = dependencyResolver.getSuperBatchDependencies();
        assertNotNull(resources);
        assertOrder(resources, superBatchResource2, superBatchResource1);
    }

    public void testGetSuperBatchDependenciesWithDependencies()
    {
        String superBatchResource1 = "test.atlassian:super1";
        String superBatchResource2 = "test.atlassian:super2";

        superBatchKeys.add(superBatchResource1);
        superBatchKeys.add(superBatchResource2);
        mockWebResourceIntegration.matchAndReturn("getSuperBatchVersion", "1.0");

        // dependcies
        String resourceA = "test.atlassian:a";
        String resourceB = "test.atlassian:b";
        String resourceC = "test.atlassian:c";
        String resourceD = "test.atlassian:d";

        // super batch 1 depends on A, B
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceA)),
            TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceB)),
            TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(superBatchResource1)),
            TestUtils.createWebResourceModuleDescriptor(superBatchResource1, testPlugin, Collections.EMPTY_LIST, Arrays.asList(resourceA, resourceB)));

        // super batch 2 depends on C
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceD)),
            TestUtils.createWebResourceModuleDescriptor(resourceD, testPlugin));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceC)),
            TestUtils.createWebResourceModuleDescriptor(resourceC, testPlugin, Collections.EMPTY_LIST, Arrays.asList(resourceD)));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(superBatchResource2)),
            TestUtils.createWebResourceModuleDescriptor(superBatchResource2, testPlugin, Collections.EMPTY_LIST, Collections.singletonList(resourceC)));

        LinkedHashSet<String> resources = dependencyResolver.getSuperBatchDependencies();
        assertNotNull(resources);
        assertOrder(resources, resourceA, resourceB, superBatchResource1, resourceD, resourceC, superBatchResource2);
    }

    public void testGetDependenciesExcludesSuperBatch()
    {
        String superBatchResource1 = "test.atlassian:super1";
        String superBatchResource2 = "test.atlassian:super2";
        String moduleKey = "test.atlassian:foo";

        superBatchKeys.add(superBatchResource1);
        superBatchKeys.add(superBatchResource2);
        mockWebResourceIntegration.matchAndReturn("getSuperBatchVersion", "1.0");

        // module depends on super batch 1
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(moduleKey)),
            TestUtils.createWebResourceModuleDescriptor(moduleKey, testPlugin, Collections.EMPTY_LIST, Arrays.asList(superBatchResource1)));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(superBatchResource1)),
            TestUtils.createWebResourceModuleDescriptor(superBatchResource1, testPlugin));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(superBatchResource2)),
            TestUtils.createWebResourceModuleDescriptor(superBatchResource2, testPlugin));

        LinkedHashSet<String> resources = dependencyResolver.getDependencies(moduleKey, true);
        assertNotNull(resources);
        assertOrder(resources, moduleKey);
    }

    public void testGetDependenciesIncludesSuperBatch()
    {
        String superBatchResource1 = "test.atlassian:super1";
        String superBatchResource2 = "test.atlassian:super2";
        String moduleKey = "test.atlassian:foo";

        superBatchKeys.add(superBatchResource1);
        superBatchKeys.add(superBatchResource2);
        mockWebResourceIntegration.matchAndReturn("getSuperBatchVersion", "1.0");

        // module depends on super batch 1
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(moduleKey)),
            TestUtils.createWebResourceModuleDescriptor(moduleKey, testPlugin, Collections.EMPTY_LIST, Arrays.asList(superBatchResource1)));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(superBatchResource1)),
            TestUtils.createWebResourceModuleDescriptor(superBatchResource1, testPlugin));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(superBatchResource2)),
            TestUtils.createWebResourceModuleDescriptor(superBatchResource2, testPlugin));

        LinkedHashSet<String> resources = dependencyResolver.getDependencies(moduleKey, false);
        assertNotNull(resources);
        assertOrder(resources, superBatchResource1, moduleKey);
    }

    private void assertOrder(LinkedHashSet<String> resources, String... expectedResources)
    {
        assertEquals(resources.size(), expectedResources.length);

        int i = 0;
        for(String resource : resources)
        {
            assertEquals(expectedResources[i], resource);
            i++;
        }
    }
}
