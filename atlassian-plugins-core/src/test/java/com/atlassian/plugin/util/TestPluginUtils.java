package com.atlassian.plugin.util;

import junit.framework.TestCase;
import com.atlassian.plugin.descriptors.MockUnusedModuleDescriptor;
import com.atlassian.plugin.descriptors.RequiresRestart;
import com.atlassian.plugin.Plugin;
import static com.atlassian.plugin.util.validation.ValidatePattern.createPattern;
import static com.atlassian.plugin.util.validation.ValidatePattern.test;
import com.mockobjects.dynamic.Mock;

import java.util.Arrays;

public class TestPluginUtils extends TestCase
{
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
