package com.atlassian.plugin.metadata;

import junit.framework.TestCase;

import com.atlassian.plugin.MockModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.descriptors.CannotDisable;

public class TestDefaultPluginMetadataManager extends TestCase
{
    public void testIsUserInstalledPlugin()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        assertTrue(new DefaultPluginMetadataManager(empty()).isUserInstalled(plugin));
    }

    public void testIsNotUserInstalledPluginIfApplicationSupplied()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        assertFalse(new DefaultPluginMetadataManager(application()).isUserInstalled(plugin));
    }

    public void testPluginIsNotUserInstalledBecauseItIsBundled()
    {
        final Plugin plugin = new MockBundledPlugin("my.plugin");
        assertFalse(new DefaultPluginMetadataManager(empty()).isUserInstalled(plugin));
    }

    public void testPluginIsRequired()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        assertFalse(new DefaultPluginMetadataManager(requiredPlugin()).isOptional(plugin));
    }

    public void testPluginIsOptional()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        assertTrue(new DefaultPluginMetadataManager(empty()).isOptional(plugin));
    }

    public void testPluginWithModulesIsOptional()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        final ModuleDescriptor<?> moduleDescriptor = new MockModuleDescriptor<Object>(plugin, "my.plugin.c-mod3", null);
        plugin.addModuleDescriptor(moduleDescriptor);
        assertTrue(new DefaultPluginMetadataManager(empty()).isOptional(plugin));
    }

    public void testPluginIsRequiredBecauseOfRequiredModule()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        final ModuleDescriptor<?> moduleDescriptor = new MockModuleDescriptor<Object>(plugin, "my.plugin.c-mod3", null);
        plugin.addModuleDescriptor(moduleDescriptor);
        assertFalse(new DefaultPluginMetadataManager(requiredModule()).isOptional(plugin));
    }

    public void testModuleIsRequired()
    {
        final ModuleDescriptor<?> moduleDescriptor = new MockModuleDescriptor<Object>(null, "my.plugin.c-mod3", null);
        assertFalse(new DefaultPluginMetadataManager(requiredModule()).isOptional(moduleDescriptor));
    }

    public void testModuleIsRequiredBecauseParentPluginIsRequired()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        final ModuleDescriptor<?> moduleDescriptor = new MockModuleDescriptor<Object>(plugin, "my.plugin.c-mod3", null);
        plugin.addModuleDescriptor(moduleDescriptor);
        assertFalse(new DefaultPluginMetadataManager(requiredPlugin()).isOptional(moduleDescriptor));
    }

    public void testModuleIsNotMadeRequiredBecauseSiblingModuleIsRequired()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        final MockModuleDescriptor<Object> required = new MockModuleDescriptor<Object>(plugin, "required", null);
        plugin.addModuleDescriptor(required);
        final ModuleDescriptor<?> moduleDescriptor = new MockModuleDescriptor<Object>(plugin, "not-required", null);
        plugin.addModuleDescriptor(moduleDescriptor);
        assertTrue(new DefaultPluginMetadataManager(of(new EmptyPluginMetadata()
        {
            @Override
            public boolean required(final ModuleDescriptor<?> descriptor)
            {
                return descriptor == required;
            }
        })).isOptional(moduleDescriptor));
    }

    public void testModuleIsRequiredTypeMarkedByAnnotation()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        // Looked a bit, do not think mockito can create a mock such that it has
        // a class-level annotation
        final ModuleDescriptor<?> moduleDescriptor = new CannotDisableModuleDescriptorType(plugin, "my.plugin.c-mod3");

        assertFalse(new DefaultPluginMetadataManager(empty()).isOptional(moduleDescriptor));
    }

    public void testModuleIsOptional()
    {
        final Plugin plugin = new MockPlugin("my.plugin");
        final ModuleDescriptor<?> moduleDescriptor = new MockModuleDescriptor<Object>(plugin, "my.plugin.c-mod3", null);
        plugin.addModuleDescriptor(moduleDescriptor);
        assertTrue(new DefaultPluginMetadataManager(empty()).isOptional(moduleDescriptor));
    }

    public void testIsOptionalPluginNullPlugin()
    {
        try
        {
            new DefaultPluginMetadataManager(empty()).isOptional((Plugin) null);
            fail("Expected NPE");
        }
        catch (final NullPointerException expected)
        {}
    }

    public void testIsOptionalModuleNullModule()
    {
        try
        {
            new DefaultPluginMetadataManager(empty()).isOptional((ModuleDescriptor<?>) null);
            fail("Expected NPE");
        }
        catch (final NullPointerException expected)
        {}
    }

    public void testIsUserInstalledPluginNullPlugin()
    {
        try
        {
            new DefaultPluginMetadataManager(empty()).isUserInstalled(null);
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

    static PluginMetadata.Factory empty()
    {
        return new PluginMetadata.Factory()
        {
            public PluginMetadata get()
            {
                return new EmptyPluginMetadata();
            }
        };
    }

    static PluginMetadata.Factory application()
    {
        return of(new EmptyPluginMetadata()
        {
            @Override
            public boolean applicationProvided(final Plugin plugin)
            {
                return true;
            }
        });
    }

    static PluginMetadata.Factory requiredPlugin()
    {
        return of(new EmptyPluginMetadata()
        {
            @Override
            public boolean required(final Plugin plugin)
            {
                return true;
            }
        });
    }

    static PluginMetadata.Factory requiredModule()
    {
        return of(new EmptyPluginMetadata()
        {
            @Override
            public boolean required(final Plugin plugin)
            {
                return true;
            }
        });
    }

    static PluginMetadata.Factory of(final PluginMetadata metadata)
    {
        return new PluginMetadata.Factory()
        {
            public PluginMetadata get()
            {
                return metadata;
            }
        };
    }

    static class EmptyPluginMetadata implements PluginMetadata
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

    class ModuleRequiredPluginMetadata extends EmptyPluginMetadata
    {
        @Override
        public boolean required(final ModuleDescriptor<?> descriptor)
        {
            return true;
        }
    }
}
