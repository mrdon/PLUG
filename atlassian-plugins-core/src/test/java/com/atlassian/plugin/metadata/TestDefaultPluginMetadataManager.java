package com.atlassian.plugin.metadata;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.CannotDisable;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.util.concurrent.NotNull;
import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;
import org.dom4j.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestDefaultPluginMetadataManager extends TestCase
{
    private final Map<String, InputStream> testData;
    private static final String applicationProvidedPlugins = "my.plugin.a\nmy.plugin.b\nmy.plugin.c\n  my.plugin.with.whitespace  ";
    private static final String requiredPlugins = "my.plugin.a\nmy.plugin.b";
    private static final String requiredModules = "my.plugin.a-mod1\nmy.plugin.c-mod1\n   \n  #hello \nmy.plugin.c-mod2";
    private DefaultPluginMetadataManager pluginMetadataManager;

    public TestDefaultPluginMetadataManager()
    {
        testData = new HashMap<String, InputStream>();
        try
        {
            testData.put(DefaultPluginMetadataManager.APPLICATION_PROVIDED_PLUGINS_FILENAME_PREFIX
                    + DefaultPluginMetadataManager.DOT
                    + DefaultPluginMetadataManager.FILENAME_EXTENSION,
                    new ByteArrayInputStream(applicationProvidedPlugins.getBytes("UTF-8")));
            testData.put(DefaultPluginMetadataManager.REQUIRED_PLUGINS_FILENAME_PREFIX
                    + DefaultPluginMetadataManager.DOT
                    + DefaultPluginMetadataManager.FILENAME_EXTENSION,
                    new ByteArrayInputStream(requiredPlugins.getBytes("UTF-8")));
            testData.put(DefaultPluginMetadataManager.REQUIRED_MODULES_FILENAME_PREFIX
                    + DefaultPluginMetadataManager.DOT
                    + DefaultPluginMetadataManager.FILENAME_EXTENSION,
                    new ByteArrayInputStream(requiredModules.getBytes("UTF-8")));
        }
        catch (UnsupportedEncodingException e)
        {
            fail("Unable to construct test data");
        }
    }

    public void setUp() throws IOException
    {
        pluginMetadataManager = new DefaultPluginMetadataManager()
        {
            @Override
            InputStream getInputStreamFromClasspath(final String fileName)
            {
                return testData.get(fileName);
            }
        };
    }

    public void tearDown() throws IOException
    {
        pluginMetadataManager = null;
    }

    public void testIsUserInstalledPluginPluginFromUser()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.d");
        when(plugin.isBundledPlugin()).thenReturn(false);
        assertTrue(pluginMetadataManager.isUserInstalled(plugin));
    }

    public void testIsUserInstalledPluginPluginFromSystem()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.a");
        when(plugin.isBundledPlugin()).thenReturn(false);
        assertFalse(pluginMetadataManager.isUserInstalled(plugin));
    }

    public void testPluginIsNotUserInstalledBecauseItIsBundled()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.d");
        when(plugin.isBundledPlugin()).thenReturn(true);
        assertFalse(pluginMetadataManager.isUserInstalled(plugin));
    }

    public void testPluginIsOptional()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.c");
        assertTrue(pluginMetadataManager.isOptional(plugin));
    }

    public void testPluginWithModulesIsOptional()
    {
        final Plugin plugin = mock(Plugin.class);
        final ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleDescriptor.getCompleteKey()).thenReturn("my.plugin.c-mod3");
        when(plugin.getKey()).thenReturn("my.plugin.c");
        when(plugin.getModuleDescriptors()).thenReturn(Collections.<ModuleDescriptor<?>>singleton(moduleDescriptor));
        assertTrue(pluginMetadataManager.isOptional(plugin));
    }
    
    public void testPluginIsRequired()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.a");
        assertFalse(pluginMetadataManager.isOptional(plugin));
    }

    public void testPluginIsRequiredBecauseOfRequiredModule()
    {
        final Plugin plugin = mock(Plugin.class);
        final ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleDescriptor.getCompleteKey()).thenReturn("my.plugin.c-mod1");
        when(plugin.getKey()).thenReturn("my.plugin.c");
        when(plugin.getModuleDescriptors()).thenReturn(Collections.<ModuleDescriptor<?>>singleton(moduleDescriptor));

        assertFalse(pluginMetadataManager.isOptional(plugin));
    }
    
    public void testModuleIsRequired()
    {
        final ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleDescriptor.getCompleteKey()).thenReturn("my.plugin.a-mod1");
        assertFalse(pluginMetadataManager.isOptional(moduleDescriptor));
    }

    public void testModuleIsRequiredBecauseParentPluginIsRequired()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.a");
        final ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleDescriptor.getCompleteKey()).thenReturn("my.plugin.a-mod3");
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        assertFalse(pluginMetadataManager.isOptional(moduleDescriptor));
    }

    public void testModuleIsNotMadeRequiredBecauseSiblingModuleIsRequired()
    {
        final Plugin plugin = mock(Plugin.class);
        // Plugin is not required
        when(plugin.getKey()).thenReturn("my.plugin.d");
        final ModuleDescriptor moduleDescriptor1 = mock(ModuleDescriptor.class);
        // Module 1 is not required
        when(moduleDescriptor1.getCompleteKey()).thenReturn("my.plugin.a-mod3");
        when(moduleDescriptor1.getPlugin()).thenReturn(plugin);
        // Module 2 is required
        final ModuleDescriptor moduleDescriptor2 = mock(ModuleDescriptor.class);
        when(moduleDescriptor2.getCompleteKey()).thenReturn("my.plugin.a-mod1");
        when(moduleDescriptor2.getPlugin()).thenReturn(plugin);
        when(plugin.getModuleDescriptors()).thenReturn(ImmutableList.<ModuleDescriptor<?>>of(moduleDescriptor1, moduleDescriptor2));
        assertTrue(pluginMetadataManager.isOptional(moduleDescriptor1));
    }

    public void testModuleIsRequiredTypeMarkedByAnnotation()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.c");
        // Looked a bit, do not think mockito can create a mock such that it has a class-level annotation
        final ModuleDescriptor moduleDescriptor = new CannotDisableModuleDescriptorType("my.plugin.c-mod3", plugin);
        
        assertFalse(pluginMetadataManager.isOptional(moduleDescriptor));
    }

    public void testModuleIsOptional()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.d");
        final ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleDescriptor.getCompleteKey()).thenReturn("my.plugin.a-mod3");
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        assertTrue(pluginMetadataManager.isOptional(moduleDescriptor));
    }

    public void testIsOptionalPluginNullPlugin()
    {
        try
        {
            pluginMetadataManager.isOptional((Plugin) null);
            fail("Expected NPE");
        }
        catch (NullPointerException e)
        {
            // expected
        }
    }

    public void testIsOptionalModuleNullModule()
    {
        try
        {
            pluginMetadataManager.isOptional((ModuleDescriptor) null);
            fail("Expected NPE");
        }
        catch (NullPointerException e)
        {
            // expected
        }
    }

    public void testIsUserInstalledPluginNullPlugin()
    {
        try
        {
            pluginMetadataManager.isUserInstalled(null);
            fail("Expected NPE");
        }
        catch (NullPointerException e)
        {
            // expected
        }
    }

    public void testIsUserInstalledPluginNoFileOnClasspath()
    {
        pluginMetadataManager = new DefaultPluginMetadataManager()
        {
            @Override
            InputStream getInputStreamFromClasspath(final String fileName)
            {
                return null;
            }
        };

        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.a");
        assertTrue(pluginMetadataManager.isUserInstalled(plugin));
    }

    public void testIsUserInstalledPluginPluginSpecifiedWithWhitespace()
    {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("my.plugin.with.whitespace");
        assertFalse(pluginMetadataManager.isUserInstalled(plugin));
    }

    public void testBlankLinesInFilesAreNotIncluded()
    {
        // There is a blank line in the requiredModules file lets make sure that is not included
        final Plugin plugin = mock(Plugin.class);
        final ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(plugin.getKey()).thenReturn("my.plugin.d");
        when(moduleDescriptor.getCompleteKey()).thenReturn("");
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);

        assertTrue(pluginMetadataManager.isOptional(moduleDescriptor));
    }

    public void testCommentLinesInFilesAreNotIncluded()
    {
        // There is a blank line in the requiredModules file lets make sure that is not included
        final Plugin plugin = mock(Plugin.class);
        final ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(plugin.getKey()).thenReturn("my.plugin.d");
        when(moduleDescriptor.getCompleteKey()).thenReturn("#hello");
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);

        assertTrue(pluginMetadataManager.isOptional(moduleDescriptor));
    }

    public void testGetMatchingFileNamesFromClasspath()
    {
        assertEquals(4, pluginMetadataManager.getMatchingFileNamesFromClasspath("TestDefaultPluginMetadataManager", "class").length);
    }

    public void testGetMatchingFileNamesFromClasspathFindingNoneReturnsFilename()
    {
        final String[] strings = pluginMetadataManager.getMatchingFileNamesFromClasspath("MyStuff", "class");
        assertEquals(1, strings.length);
        assertEquals("MyStuff.class", strings[0]);
    }

    @CannotDisable
    private class CannotDisableModuleDescriptorType implements ModuleDescriptor<Object>
    {
        private final String completeKey;
        private final Plugin plugin;

        CannotDisableModuleDescriptorType(String completeKey, Plugin plugin)
        {
            this.completeKey = completeKey;
            this.plugin = plugin;
        }

        public String getCompleteKey()
        {
            return completeKey;
        }

        public String getPluginKey()
        {
            return null;
        }

        public String getKey()
        {
            return null;
        }

        public String getName()
        {
            return null;
        }

        public String getDescription()
        {
            return null;
        }

        public Class<Object> getModuleClass()
        {
            return null;
        }

        public Object getModule()
        {
            return null;
        }

        public void init(@NotNull final Plugin plugin, @NotNull final Element element) throws PluginParseException
        {
        }

        public boolean isEnabledByDefault()
        {
            return false;
        }

        public boolean isSystemModule()
        {
            return false;
        }

        public void destroy(final Plugin plugin)
        {
        }

        public Float getMinJavaVersion()
        {
            return null;
        }

        public boolean satisfiesMinJavaVersion()
        {
            return false;
        }

        public Map<String, String> getParams()
        {
            return null;
        }

        public String getI18nNameKey()
        {
            return null;
        }

        public String getDescriptionKey()
        {
            return null;
        }

        public Plugin getPlugin()
        {
            return plugin;
        }

        public List<ResourceDescriptor> getResourceDescriptors()
        {
            return null;
        }

        public List<ResourceDescriptor> getResourceDescriptors(final String type)
        {
            return null;
        }

        public ResourceDescriptor getResourceDescriptor(final String type, final String name)
        {
            return null;
        }

        public ResourceLocation getResourceLocation(final String type, final String name)
        {
            return null;
        }
    }
}
