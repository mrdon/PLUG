package com.atlassian.plugin.factories;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;

import java.io.File;

import junit.framework.TestCase;

public class TestLegacyDynamicPluginFactory extends TestCase
{
    public void testCreateCorruptJar()
    {
        final LegacyDynamicPluginFactory factory = new LegacyDynamicPluginFactory(PluginAccessor.Descriptor.FILENAME);
        final Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        try
        {
            Mock mockArtifact = new Mock(PluginArtifact.class);
            mockArtifact.expectAndReturn("getResourceAsStream", C.ANY_ARGS, null);
            mockArtifact.expectAndReturn("toFile", new File("sadfasdf"));
            factory.create((PluginArtifact) mockArtifact.proxy(), (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy());
            fail("Should have thrown an exception");
        }
        catch (final PluginParseException ex)
        {
            // horray!
        }
        catch (final Exception ex)
        {
            ex.printStackTrace();
            fail("No exceptions allowed");
        }
    }
}
