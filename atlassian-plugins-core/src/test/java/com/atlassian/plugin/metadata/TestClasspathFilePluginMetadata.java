package com.atlassian.plugin.metadata;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestClasspathFilePluginMetadata extends TestCase
{
    private final Map<String, Collection<InputStream>> testData;
    private static final String applicationProvidedPlugins = "my.plugin.a\nmy.plugin.b\nmy.plugin.c\n  my.plugin.with.whitespace  ";
    private static final String applicationProvidedPlugins2 = "my.plugin.z";
    private static final String requiredPlugins = "my.plugin.a\nmy.plugin.b";
    private static final String requiredModules = "my.plugin.a-mod1\nmy.plugin.c-mod1\n   \n  #hello \nmy.plugin.c-mod2";
    private PluginMetadata pluginMetadata;

    public TestClasspathFilePluginMetadata()
    {
        testData = new HashMap<String, Collection<InputStream>>();
        testData.put(ClasspathFilePluginMetadata.APPLICATION_PROVIDED_PLUGINS_FILENAME, toStreams(applicationProvidedPlugins, applicationProvidedPlugins2));
        testData.put(ClasspathFilePluginMetadata.APPLICATION_REQUIRED_PLUGINS_FILENAME, toStreams(requiredPlugins));
        testData.put(ClasspathFilePluginMetadata.APPLICATION_REQUIRED_MODULES_FILENAME, toStreams(requiredModules));
    }

    @Override
    public void setUp() throws IOException
    {
        pluginMetadata = new ClasspathFilePluginMetadata()
        {
            // NOTE: I know that people do not like this, but I think it is WAY BETTER than faffing around with the
            // production code to the degree that it is UNREADABLE
            @Override
            Collection<InputStream> getInputStreamsForFilename(final String fileName)
            {
                return testData.get(fileName);
            }
        };
    }

    @Override
    public void tearDown() throws IOException
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
        pluginMetadata = new ClasspathFilePluginMetadata()
        {
            @Override
            Collection<InputStream> getInputStreamsForFilename(final String fileName)
            {
                return Collections.emptyList();
            }
        };

        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.a");
        assertFalse(pluginMetadata.applicationProvided(plugin));
        assertFalse(pluginMetadata.required(plugin));
    }

    public void testIsUserInstalledPluginPluginSpecifiedWithWhitespace()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.with.whitespace");
        assertTrue(pluginMetadata.applicationProvided(plugin));
    }

    public void testBlankLinesInFilesAreNotIncluded()
    {
        // There is a blank line in the requiredModules file lets make sure that
        // is not included
        final Plugin plugin = mock(Plugin.class);
        final ModuleDescriptor<?> moduleDescriptor = mock(ModuleDescriptor.class);
        when(plugin.getKey()).thenReturn("my.plugin.d");
        when(moduleDescriptor.getCompleteKey()).thenReturn("");
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);

        assertFalse(pluginMetadata.required(moduleDescriptor));
    }

    public void testCommentLinesInFilesAreNotIncluded()
    {
        // There is a blank line in the requiredModules file lets make sure that
        // is not included
        final Plugin plugin = mock(Plugin.class);
        final ModuleDescriptor<?> moduleDescriptor = mock(ModuleDescriptor.class);
        when(plugin.getKey()).thenReturn("my.plugin.d");
        when(moduleDescriptor.getCompleteKey()).thenReturn("#hello");
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);

        assertFalse(pluginMetadata.required(moduleDescriptor));
    }

    public void testPluginKeysFromSecondFileIncluded()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.z");

        assertTrue(pluginMetadata.applicationProvided(plugin));
    }

    static Collection<InputStream> toStreams(final String... names)
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
}