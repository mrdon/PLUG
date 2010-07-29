package com.atlassian.plugin.metadata;

import java.io.Reader;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;

import com.atlassian.plugin.metadata.PluginMetadata.Type;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

public class TestClasspathReaderFactory extends TestCase
{
    public void testApplicationProvidedFileName()
    {
        assertEquals("com/atlassian/plugin/metadata/application-provided-plugins.txt", new ClasspathReaderFactory().named(Type.ApplicationProvided.getFileName()));
    }

    public void testRequiredPluginsFileName()
    {
        assertEquals("com/atlassian/plugin/metadata/application-required-plugins.txt", new ClasspathReaderFactory().named(Type.RequiredPlugins.getFileName()));
    }

    public void testRequiredModuleDescriptorsFileName()
    {
        assertEquals("com/atlassian/plugin/metadata/application-required-modules.txt", new ClasspathReaderFactory().named(Type.RequiredModuleDescriptors.getFileName()));
    }

    public void testUrl()
    {
        assertNotNull(Iterators.getOnlyElement(new ClasspathReaderFactory().urls("com/atlassian/plugin/metadata/test.txt")));
    }

    public void testname() throws Exception
    {
        final Reader reader = Iterables.getOnlyElement(new ClasspathReaderFactory().getReaders(Type.ApplicationProvided));
        assertNotNull(reader);
        assertEquals("ask-the-app-and-the-app-provides", IOUtils.toString(reader));
    }
}
