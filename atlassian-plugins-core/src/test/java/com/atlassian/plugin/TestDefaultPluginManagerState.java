package com.atlassian.plugin;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.impl.StaticPlugin;

import java.util.Map;

import junit.framework.TestCase;

public class TestDefaultPluginManagerState extends TestCase
{
    private DefaultPluginManagerState state;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        state = new DefaultPluginManagerState();
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        state = null;
        super.tearDown();
    }
    
    public void testSetEnabled()
    {
        StaticPlugin plugin = createMockPlugin("mock.plugin.key", true);
        state.setEnabled(plugin, true);
        assertTrue(state.isEnabled(plugin));
        state.setEnabled(plugin, false);
        assertFalse(state.isEnabled(plugin));
        
        ModuleDescriptor<?> module = createModule("mock.plugin.key", "module.key");
        state.setEnabled(module, true);
        assertTrue(state.isEnabled(module));
        state.setEnabled(module, false);
        assertFalse(state.isEnabled(module));
    }

    public void testGetPluginStateMap()
    {
        StaticPlugin plugin1 = createMockPlugin("mock.plugin.key", true);
        StaticPlugin plugin2 = createMockPlugin("two.mock.plugin.key", true);
        ModuleDescriptor<?> module1 = createModule("mock.plugin.key", "module.key.1");
        ModuleDescriptor<?> module2 = createModule("mock.plugin.key", "module.key.2");
        ModuleDescriptor<?> module3 = createModule("mock.plugin.key", "module.key.3");
        // because all plugins and modules are enabled by default lets disable them
        
        state.setEnabled(plugin1, !plugin1.isEnabledByDefault());
        state.setEnabled(plugin2, !plugin1.isEnabledByDefault());
        state.setEnabled(module1, !module1.isEnabledByDefault());
        state.setEnabled(module2, !module2.isEnabledByDefault());
        state.setEnabled(module3, !module3.isEnabledByDefault());
        
        Map<String, Boolean> pluginStateMap = state.getPluginStateMap(plugin1);
        PluginManagerState pluginState = new DefaultPluginManagerState(pluginStateMap);
        
        assertFalse(pluginState.isEnabled(plugin1) == plugin1.isEnabledByDefault());
        assertFalse(pluginState.isEnabled(module1) == module1.isEnabledByDefault());
        assertFalse(pluginState.isEnabled(module2) == module2.isEnabledByDefault());
        assertFalse(pluginState.isEnabled(module3) == module3.isEnabledByDefault());
        // plugin2 should not be part of the map therefore it should have default enabled value
        assertTrue(pluginState.isEnabled(plugin2) == plugin2.isEnabledByDefault());
    }

    public void testDefaultModuleStateIsNotStored()
    {
        String pluginKey = "mock.plugin.key";
        StaticPlugin plugin = createMockPlugin(pluginKey, true);
        state.setEnabled(plugin, true);
        Map<String, Boolean> pluginStateMap = state.getPluginStateMap(plugin);
        assertTrue(pluginStateMap.isEmpty());

        state.setEnabled(plugin, false);
        pluginStateMap = state.getPluginStateMap(plugin);
        assertFalse(pluginStateMap.isEmpty());
        
        state.removeState(pluginKey);

        plugin = createMockPlugin(pluginKey, false);
        state.setEnabled(plugin, false);
        pluginStateMap = state.getPluginStateMap(plugin);
        assertTrue(pluginStateMap.isEmpty());
        state.setEnabled(plugin, true);
        pluginStateMap = state.getPluginStateMap(plugin);
        assertFalse(pluginStateMap.isEmpty());
    }
    
    

    private <T> ModuleDescriptor<T> createModule(final String pluginKey, final String moduleKey)
    {
        return new AbstractModuleDescriptor<T>()
        {
            @Override
            public T getModule(){ return null; }
            @Override
            public String getCompleteKey()
            {
                return pluginKey + ':' + moduleKey;
            }
        };
    }

    private StaticPlugin createMockPlugin(String pluginKey, boolean enabledByDefault)
    {
        StaticPlugin plugin = new StaticPlugin();
        plugin.setKey(pluginKey);
        plugin.setEnabledByDefault(enabledByDefault);
        return plugin;
    }
    
}
