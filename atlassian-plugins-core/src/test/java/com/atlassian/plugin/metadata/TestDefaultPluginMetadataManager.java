package com.atlassian.plugin.metadata;

import com.atlassian.plugin.MockModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.descriptors.CannotDisable;

import junit.framework.TestCase;

public class TestDefaultPluginMetadataManager extends TestCase
{
    public void testIsUserInstalledPlugin()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        assertTrue(new DefaultPluginMetadataManager(new EmptyPluginMetadata()).isUserInstalled(plugin));
    }

    public void testIsNotUserInstalledPluginIfApplicationSupplied()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        assertFalse(new DefaultPluginMetadataManager(new ApplicationPluginMetadata()).isUserInstalled(plugin));
    }

    public void testPluginIsNotUserInstalledBecauseItIsBundled()
    {
        final Plugin plugin = new MockBundledPlugin("my.plugin");
        assertFalse(new DefaultPluginMetadataManager(new EmptyPluginMetadata()).isUserInstalled(plugin));
    }

    public void testPluginIsRequired()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        assertFalse(new DefaultPluginMetadataManager(new RequiredPluginMetadata()).isOptional(plugin));
    }

    public void testPluginIsOptional()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        assertTrue(new DefaultPluginMetadataManager(new EmptyPluginMetadata()).isOptional(plugin));
    }

    public void testPluginWithModulesIsOptional()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        final ModuleDescriptor<?> moduleDescriptor = new MockModuleDescriptor<Object>(plugin, "my.plugin.c-mod3", null);
        plugin.addModuleDescriptor(moduleDescriptor);
        assertTrue(new DefaultPluginMetadataManager(new EmptyPluginMetadata()).isOptional(plugin));
    }

    public void testPluginIsRequiredBecauseOfRequiredModule()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        final ModuleDescriptor<?> moduleDescriptor = new MockModuleDescriptor<Object>(plugin, "my.plugin.c-mod3", null);
        plugin.addModuleDescriptor(moduleDescriptor);
        assertFalse(new DefaultPluginMetadataManager(new ModuleRequiredPluginMetadata()).isOptional(plugin));
    }

    public void testModuleIsRequired()
    {
        final ModuleDescriptor<?> moduleDescriptor = new MockModuleDescriptor<Object>(null, "my.plugin.c-mod3", null);
        assertFalse(new DefaultPluginMetadataManager(new ModuleRequiredPluginMetadata()).isOptional(moduleDescriptor));
    }

    public void testModuleIsRequiredBecauseParentPluginIsRequired()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        final ModuleDescriptor<?> moduleDescriptor = new MockModuleDescriptor<Object>(plugin, "my.plugin.c-mod3", null);
        plugin.addModuleDescriptor(moduleDescriptor);
        assertFalse(new DefaultPluginMetadataManager(new RequiredPluginMetadata()).isOptional(moduleDescriptor));
    }

    public void testModuleIsNotMadeRequiredBecauseSiblingModuleIsRequired()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        final MockModuleDescriptor<Object> required = new MockModuleDescriptor<Object>(plugin, "required", null);
        plugin.addModuleDescriptor(required);
        final ModuleDescriptor<?> moduleDescriptor = new MockModuleDescriptor<Object>(plugin, "not-required", null);
        plugin.addModuleDescriptor(moduleDescriptor);
        assertTrue(new DefaultPluginMetadataManager(new EmptyPluginMetadata()
        {
            @Override
            public boolean required(final ModuleDescriptor<?> descriptor)
            {
                return descriptor == required;
            }
        }).isOptional(moduleDescriptor));
    }

    public void testModuleIsRequiredTypeMarkedByAnnotation()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        // Looked a bit, do not think mockito can create a mock such that it has a class-level annotation
        final ModuleDescriptor<?> moduleDescriptor = new CannotDisableModuleDescriptorType(plugin, "my.plugin.c-mod3");

        assertFalse(new DefaultPluginMetadataManager(new EmptyPluginMetadata()).isOptional(moduleDescriptor));
    }

    public void testModuleIsOptional()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        final ModuleDescriptor<?> moduleDescriptor = new MockModuleDescriptor<Object>(plugin, "my.plugin.c-mod3", null);
        plugin.addModuleDescriptor(moduleDescriptor);
        assertTrue(new DefaultPluginMetadataManager(new EmptyPluginMetadata()).isOptional(moduleDescriptor));
    }

    public void testIsOptionalPluginNullPlugin()
    {
        try
        {
            new DefaultPluginMetadataManager(new EmptyPluginMetadata()).isOptional((Plugin) null);
            fail("Expected NPE");
        }
        catch (final NullPointerException expected)
        {}
    }

    public void testIsOptionalModuleNullModule()
    {
        try
        {
            new DefaultPluginMetadataManager(new EmptyPluginMetadata()).isOptional((ModuleDescriptor<?>) null);
            fail("Expected NPE");
        }
        catch (final NullPointerException expected)
        {}
    }

    public void testIsUserInstalledPluginNullPlugin()
    {
        try
        {
            new DefaultPluginMetadataManager(new EmptyPluginMetadata()).isUserInstalled(null);
            fail("Expected NPE");
        }
        catch (final NullPointerException expected)
        {}
    }

    class MockPlugin extends com.atlassian.plugin.MockPlugin
    {
        MockPlugin(final String key)
        {
            super(key, TestDefaultPluginMetadataManager.this.getClass().getClassLoader());
        }
    }

    class MockBundledPlugin extends MockPlugin
    {
        public MockBundledPlugin(final String key)
        {
            super(key);
        }

        @Override
        public boolean isBundledPlugin()
        {
            return true;
        }
    }

    @CannotDisable
    class CannotDisableModuleDescriptorType extends MockModuleDescriptor<Object> implements ModuleDescriptor<Object>
    {
        CannotDisableModuleDescriptorType(final Plugin plugin, final String completeKey)
        {
            super(plugin, completeKey, null);
        }
    }

    class EmptyPluginMetadata implements PluginMetadata
    {
        public boolean applicationProvided(final Plugin plugin)
        {
            return false;
        }

        public boolean required(final Plugin plugin)
        {
            return false;
        }

        public boolean required(final ModuleDescriptor<?> descriptor)
        {
            return false;
        }
    }

    class ApplicationPluginMetadata extends EmptyPluginMetadata
    {
        @Override
        public boolean applicationProvided(final Plugin plugin)
        {
            return true;
        }
    }

    class RequiredPluginMetadata extends EmptyPluginMetadata
    {
        @Override
        public boolean required(final Plugin plugin)
        {
            return true;
        }
    }

    class ModuleRequiredPluginMetadata extends EmptyPluginMetadata
    {
        @Override
        public boolean required(final ModuleDescriptor<?> descriptor)
        {
            return true;
        }
    }
}