package com.atlassian.plugin.factories;

import junit.framework.TestCase;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.mockobjects.dynamic.Mock;

import java.io.File;

public class TestLegacyDynamicPluginFactory extends TestCase
{
    public void testCreateCorruptJar()
    {
        LegacyDynamicPluginFactory factory = new LegacyDynamicPluginFactory(PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
        Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        DeploymentUnit unit = new DeploymentUnit(new File("some crap file"));
        try
        {
            factory.create(unit, (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy());
            fail("Should have thrown an exception");
        } catch (PluginParseException ex)
        {
            // horray!
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("No exceptions allowed");
        }
    }
}