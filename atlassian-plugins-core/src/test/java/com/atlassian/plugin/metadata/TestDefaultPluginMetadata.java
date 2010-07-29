package com.atlassian.plugin.metadata;

import static com.google.common.collect.ImmutableList.of;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.google.common.collect.ImmutableList;

public class TestDefaultPluginMetadata extends TestCase
{
    private static final Iterable<String> applicationProvidedPlugins = of("my.plugin.a", "my.plugin.b", "my.plugin.c", "my.plugin.with.whitespace");
    private static final Iterable<String> requiredPlugins = of("my.plugin.a", "my.plugin.b");
    private static final Iterable<String> requiredModules = of("my.plugin.a-mod1", "my.plugin.c-mod1", "my.plugin.c-mod2");
    private PluginMetadata pluginMetadata;

    @Override
    public void setUp()
    {
        pluginMetadata = new DefaultPluginMetadata(applicationProvidedPlugins, requiredPlugins, requiredModules);
    }

    @Override
    public void tearDown()
    {
        pluginMetadata = null;
    }

    public void testIsUserInstalledPluginPluginFromUser()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.d");
        assertFalse(pluginMetadata.applicationProvided(plugin));
    }

    public void testIsUserInstalledPluginPluginFromSystem()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.a");
        assertTrue(pluginMetadata.applicationProvided(plugin));
    }

    public void testPluginRequired()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.a");
        assertTrue(pluginMetadata.required(plugin));
    }

    public void testPluginNotRequired()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.x");
        assertFalse(pluginMetadata.applicationProvided(plugin));
    }

    public void testModuleRequired()
    {
        final ModuleDescriptor<?> moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleDescriptor.getCompleteKey()).thenReturn("my.plugin.c-mod2");
        assertTrue(pluginMetadata.required(moduleDescriptor));
    }

    public void testModuleNotRequired()
    {
        final ModuleDescriptor<?> moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleDescriptor.getCompleteKey()).thenReturn("my.plugin.c-mod3");
        assertFalse(pluginMetadata.required(moduleDescriptor));
    }

    public void testModuleIsRequired()
    {
        final ModuleDescriptor<?> moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleDescriptor.getCompleteKey()).thenReturn("my.plugin.a-mod1");
        assertTrue(pluginMetadata.required(moduleDescriptor));
    }

    public void testApplicationProvidedPluginNullPlugin()
    {
        try
        {
            pluginMetadata.applicationProvided(null);
            fail("Expected NPE");
        }
        catch (final NullPointerException expected)
        {}
    }

    public void testRequiredPluginNullPlugin()
    {
        try
        {
            pluginMetadata.required((Plugin) null);
            fail("Expected NPE");
        }
        catch (final NullPointerException expected)
        {}
    }

    public void testRequiredModuleNullModule()
    {
        try
        {
            pluginMetadata.required((ModuleDescriptor<?>) null);
            fail("Expected NPE");
        }
        catch (final NullPointerException expected)
        {}
    }

    public void testIsUserInstalledPluginNoFileOnClasspath()
    {
        pluginMetadata = new DefaultPluginMetadata(empty(), empty(), empty());

        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.a");
        assertFalse(pluginMetadata.applicationProvided(plugin));
        assertFalse(pluginMetadata.required(plugin));
    }

    static Iterable<InputStream> toStreams(final String... names)
    {
        final ImmutableList.Builder<InputStream> builder = ImmutableList.builder();
        for (final String name : names)
        {
            try
            {
                builder.add(new ByteArrayInputStream(name.getBytes("UTF-8")));
            }
            catch (final UnsupportedEncodingException e)
            {
                throw new Error("Unable to construct test data", e);
            }
        }
        return builder.build();
    }

    static Iterable<String> empty()
    {
        return ImmutableList.of();
    }
}
