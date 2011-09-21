package com.atlassian.plugin.metadata;

import com.atlassian.plugin.PluginAccessor;
import junit.framework.TestCase;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;

public class TestRequiredPluginValidator extends TestCase
{
    private static final String PLUGIN_KEY = "com.atlassian.req.plugin.1";

    private static final Set<String> REQ_PLUGINS = new HashSet<String>()
    {{
        add(PLUGIN_KEY);
        add("com.atlassian.req.plugin.2");
        add("com.atlassian.req.plugin.3");
    }};

    private static final Set<String> REQ_PLUGIN_MODULES = new HashSet<String>()
    {{
        add("com.atlassian.req.plugin.4:module1");
        add("com.atlassian.req.plugin.4:module2");
        add("com.atlassian.req.plugin.4:module3");
    }};

    private RequiredPluginProvider provider;

    @Mock
    private PluginAccessor pluginAccessor;

    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        
        provider = new RequiredPluginProvider()
        {
            public Set<String> getRequiredPluginKeys()
            {
                return REQ_PLUGINS;
            }

            public Set<String> getRequiredModuleKeys()
            {
                return REQ_PLUGIN_MODULES;
            }
        };
    }

    public void testValidate()
    {
        Mockito.when(pluginAccessor.isPluginEnabled(Mockito.anyString())).thenReturn(true);
        Mockito.when(pluginAccessor.isPluginModuleEnabled(Mockito.anyString())).thenReturn(true);
        RequiredPluginValidator validator = new RequiredPluginValidator(pluginAccessor, provider);
        assertTrue(validator.validate());
        assertTrue(validator.getErrors().isEmpty());
    }

    public void testValidateFails()
    {
        Mockito.when(pluginAccessor.isPluginEnabled(Mockito.anyString())).thenReturn(true);
        Mockito.when(pluginAccessor.isPluginEnabled(PLUGIN_KEY)).thenReturn(false);
        Mockito.when(pluginAccessor.isPluginModuleEnabled(Mockito.anyString())).thenReturn(true);
        RequiredPluginValidator validator = new RequiredPluginValidator(pluginAccessor, provider);
        assertFalse(validator.validate());
        assertTrue(validator.getErrors().contains(PLUGIN_KEY));
        assertEquals(1, validator.getErrors().size());
    }
}
