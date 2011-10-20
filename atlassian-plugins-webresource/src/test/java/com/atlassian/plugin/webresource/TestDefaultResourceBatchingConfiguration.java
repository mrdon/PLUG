package com.atlassian.plugin.webresource;

import com.atlassian.plugin.util.PluginUtils;
import junit.framework.TestCase;

public class TestDefaultResourceBatchingConfiguration extends TestCase
{
    private DefaultResourceBatchingConfiguration batchingConfiguration;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        batchingConfiguration = new DefaultResourceBatchingConfiguration();
    }

    @Override
    public void tearDown() throws Exception
    {
        batchingConfiguration = null;
        System.clearProperty(DefaultResourceBatchingConfiguration.PLUGIN_WEBRESOURCE_BATCHING_OFF);
        System.clearProperty(PluginUtils.ATLASSIAN_DEV_MODE);
        super.tearDown();
    }

    public void testBatchingIsEnabledWhenNotInDevModeAndBatchingIsNotOff()
    {
        assertTrue(batchingConfiguration.isPluginWebResourceBatchingEnabled());
    }

    public void testBatchingIsDisabledWhenInDevMode()
    {
        System.clearProperty(DefaultResourceBatchingConfiguration.PLUGIN_WEBRESOURCE_BATCHING_OFF);
        System.setProperty(PluginUtils.ATLASSIAN_DEV_MODE, "true");
        assertFalse(batchingConfiguration.isPluginWebResourceBatchingEnabled());
    }
    
    public void testBatchingIsDisabledWhenBatchingIsOff()
    {
        System.setProperty(DefaultResourceBatchingConfiguration.PLUGIN_WEBRESOURCE_BATCHING_OFF, "true");
        System.clearProperty(PluginUtils.ATLASSIAN_DEV_MODE);
        assertFalse(batchingConfiguration.isPluginWebResourceBatchingEnabled());
    }
    
    public void testBatchingIsEnabledWhenDevModeIsFalse()
    {
        System.clearProperty(DefaultResourceBatchingConfiguration.PLUGIN_WEBRESOURCE_BATCHING_OFF);
        System.setProperty(PluginUtils.ATLASSIAN_DEV_MODE, "false");
        assertTrue(batchingConfiguration.isPluginWebResourceBatchingEnabled());
    }
    
    public void testBatchingIsEnabledWhenInDevModeAndBatchingIsExplicitlyNotOff()
    {
        System.setProperty(DefaultResourceBatchingConfiguration.PLUGIN_WEBRESOURCE_BATCHING_OFF, "false");
        System.setProperty(PluginUtils.ATLASSIAN_DEV_MODE, "true");
        assertTrue(batchingConfiguration.isPluginWebResourceBatchingEnabled());
    }
}
