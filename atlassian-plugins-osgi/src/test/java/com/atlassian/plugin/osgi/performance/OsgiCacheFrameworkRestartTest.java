package com.atlassian.plugin.osgi.performance;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.PluginInContainerTestBase;
import com.atlassian.plugin.osgi.DummyModuleDescriptor;
import com.atlassian.plugin.osgi.SomeInterface;
import com.atlassian.plugin.test.PluginJarBuilder;

import org.apache.commons.io.FileUtils;

import java.io.IOException;

/**
 * Tests the plugin framework handling restarts correctly
 */
public class OsgiCacheFrameworkRestartTest extends OsgiNoCacheFrameworkRestartTest
{
    @Override
    protected void startPluginFramework() throws Exception
    {
        initPluginManager(prov, factory, "1.0");
    }
}