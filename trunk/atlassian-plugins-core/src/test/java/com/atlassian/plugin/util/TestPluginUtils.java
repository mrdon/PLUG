package com.atlassian.plugin.util;

import junit.framework.TestCase;
import com.atlassian.plugin.descriptors.MockUnusedModuleDescriptor;
import com.atlassian.plugin.descriptors.RequiresRestart;
import com.atlassian.plugin.Plugin;
import com.mockobjects.dynamic.Mock;

import java.util.Arrays;

public class TestPluginUtils extends TestCase
{
    public void testDoesPluginRequireRestartDevMode()
    {
        try
        {
            System.setProperty("atlassian.dev.mode", "true");
            Mock mockPlugin = new Mock(Plugin.class);
            assertFalse(PluginUtils.doesPluginRequireRestart((Plugin) mockPlugin.proxy()));
            mockPlugin.verify();
        }
        finally
        {
            System.clearProperty("atlassian.dev.mode");
        }

        Mock mockPlugin2 = new Mock(Plugin.class);
        mockPlugin2.expectAndReturn("getModuleDescriptors", Arrays.asList(new DynamicModuleDescriptor(), new RequiresRestartModuleDescriptor()));
        assertTrue(PluginUtils.doesPluginRequireRestart((Plugin) mockPlugin2.proxy()));
        mockPlugin2.verify();                
    }

    public void testDoesPluginRequireRestart()
    {
        Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.expectAndReturn("getModuleDescriptors", Arrays.asList(new DynamicModuleDescriptor(), new RequiresRestartModuleDescriptor()));
        assertTrue(PluginUtils.doesPluginRequireRestart((Plugin) mockPlugin.proxy()));
        mockPlugin.verify();

        mockPlugin = new Mock(Plugin.class);
        mockPlugin.expectAndReturn("getModuleDescriptors", Arrays.asList(new DynamicModuleDescriptor()));
        assertFalse(PluginUtils.doesPluginRequireRestart((Plugin) mockPlugin.proxy()));
        mockPlugin.verify();

        mockPlugin = new Mock(Plugin.class);
        mockPlugin.expectAndReturn("getModuleDescriptors", Arrays.asList());
        assertFalse(PluginUtils.doesPluginRequireRestart((Plugin) mockPlugin.proxy()));
        mockPlugin.verify();

        mockPlugin = new Mock(Plugin.class);
        mockPlugin.expectAndReturn("getModuleDescriptors", Arrays.asList(new RequiresRestartModuleDescriptor()));
        assertTrue(PluginUtils.doesPluginRequireRestart((Plugin) mockPlugin.proxy()));
        mockPlugin.verify();
    }

    static class DynamicModuleDescriptor extends MockUnusedModuleDescriptor
    {}

    @RequiresRestart
    static class RequiresRestartModuleDescriptor extends MockUnusedModuleDescriptor {}
}
