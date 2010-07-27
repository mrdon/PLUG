package com.atlassian.plugin.metadata;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;

import com.google.common.base.Function;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class TestClasspathPluginMetadata extends TestCase
{
    private final Map<String, InputStream> testData;
    private static final String applicationProvidedPlugins = "my.plugin.a\nmy.plugin.b\nmy.plugin.c\n  my.plugin.with.whitespace  ";
    private static final String requiredPlugins = "my.plugin.a\nmy.plugin.b";
    private static final String requiredModules = "my.plugin.a-mod1\nmy.plugin.c-mod1\n   \n  #hello \nmy.plugin.c-mod2";
    private PluginMetadata pluginMetadata;

    public TestClasspathPluginMetadata()
    {
        testData = new HashMap<String, InputStream>();
        try
        {
            testData.put(
                ClasspathPluginMetadata.APPLICATION_PROVIDED_PLUGINS_FILENAME_PREFIX + ClasspathPluginMetadata.DOT + ClasspathPluginMetadata.FILENAME_EXTENSION,
                new ByteArrayInputStream(applicationProvidedPlugins.getBytes("UTF-8")));
            testData.put(
                ClasspathPluginMetadata.REQUIRED_PLUGINS_FILENAME_PREFIX + ClasspathPluginMetadata.DOT + ClasspathPluginMetadata.FILENAME_EXTENSION,
                new ByteArrayInputStream(requiredPlugins.getBytes("UTF-8")));
            testData.put(
                ClasspathPluginMetadata.REQUIRED_MODULES_FILENAME_PREFIX + ClasspathPluginMetadata.DOT + ClasspathPluginMetadata.FILENAME_EXTENSION,
                new ByteArrayInputStream(requiredModules.getBytes("UTF-8")));
        }
        catch (final UnsupportedEncodingException e)
        {
            fail("Unable to construct test data");
        }
    }

    @Override
    public void setUp() throws IOException
    {
        pluginMetadata = new ClasspathPluginMetadata(new Function<String, InputStream>()
        {
            public InputStream apply(final String fileName)
            {
                return testData.get(fileName);
            }
        });
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
        pluginMetadata = new ClasspathPluginMetadata(new Function<String, InputStream>()
        {
            public InputStream apply(final String fileName)
            {
                return null;
            }
        });

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
        // There is a blank line in the requiredModules file lets make sure that is not included
        final Plugin plugin = mock(Plugin.class);
        final ModuleDescriptor<?> moduleDescriptor = mock(ModuleDescriptor.class);
        when(plugin.getKey()).thenReturn("my.plugin.d");
        when(moduleDescriptor.getCompleteKey()).thenReturn("");
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);

        assertFalse(pluginMetadata.required(moduleDescriptor));
    }

    public void testCommentLinesInFilesAreNotIncluded()
    {
        // There is a blank line in the requiredModules file lets make sure that is not included
        final Plugin plugin = mock(Plugin.class);
        final ModuleDescriptor<?> moduleDescriptor = mock(ModuleDescriptor.class);
        when(plugin.getKey()).thenReturn("my.plugin.d");
        when(moduleDescriptor.getCompleteKey()).thenReturn("#hello");
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);

        assertFalse(pluginMetadata.required(moduleDescriptor));
    }

    public void testGetMatchingFileNamesFromClasspath()
    {
        // total of this class + inner classes
        assertEquals(3, ClasspathPluginMetadata.getMatchingFileNamesFromClasspath("TestClasspathPluginMetadata", "class").length);
    }

    public void testGetMatchingFileNamesFromClasspathFindingNoneReturnsFilename()
    {
        final String[] strings = ClasspathPluginMetadata.getMatchingFileNamesFromClasspath("MyStuff", "class");
        assertEquals(1, strings.length);
        assertEquals("MyStuff.class", strings[0]);
    }
}
