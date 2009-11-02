package com.atlassian.plugin.webresource;

import junit.framework.TestCase;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import com.atlassian.plugin.PluginAccessor;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class TestDefaultResourceDependencyResolver extends TestCase
{
    private Mock mockWebResourceIntegration;
    private Mock mockPluginAccessor;
    private ResourceDependencyResolver dependencyResolver;
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

        super.tearDown();
    }

    public void testSimpleGetSuperBatchDependencies()
    {
//        String superBatchResource1 = "plugin.key:resource1";
//        String supertBatchResource2 = "plugin.key:resource2";
//
//        superBatchKeys.add(superBatchResource1);
//        superBatchKeys.add(supertBatchResource2);
//
//        mockWebResourceIntegration.expectAndReturn("");
//
//        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(superBatchResource1)),
//            TestUtils.createWebResourceModuleDescriptor(superBatchResource1, TestUtils.createTestPlugin()));
//
//        LinkedHashSet<String> resources = dependencyResolver.getSuperBatchDependencies();
//        assertNotNull(resources);
//        assertEquals(2, resources.size());
    }
}
