package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.test.PluginJarBuilder;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

public class TestTransformStageUtils extends TestCase
{
    public void testExtractPluginClasses() throws Exception
    {
        final File pluginFile = new PluginJarBuilder()
            .addFormattedJava("my.Foo",
                    "package my;",
                    "public class Foo {",
                    "  com.atlassian.plugin.osgi.factory.transform.Fooable bar;",
                    "}")
            .addFormattedJava("com.atlassian.plugin.osgi.SomeInterface",
                              "package com.atlassian.plugin.osgi;",
                              "public interface SomeInterface {}")
            .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='plugin1' key='first' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "    </plugin-info>",
                    "<component key='component1' class='my.Foo'/>",
                    "<component-import key='component1'>",
                    "    <interface>com.atlassian.plugin.osgi.SomeInterface</interface>",
                    "</component-import>",
                    "</atlassian-plugin>")
            .build();

        Set<String> classes = TransformStageUtils.extractPluginClasses(new FileInputStream(pluginFile));

        // Only classes should be included in the result set.
        assertEquals(2, classes.size());
        assertTrue(classes.contains("my.Foo"));
        assertTrue(classes.contains("com.atlassian.plugin.osgi.SomeInterface"));
    }

    public void testGetPackageName()
    {
        assertEquals("com.atlassian.plugin.osgi", TransformStageUtils.getPackageName("com.atlassian.plugin.osgi.SomeInterface"));
        assertEquals("java.lang", TransformStageUtils.getPackageName("java.lang.Class"));
        assertEquals("com.hello.world", TransformStageUtils.getPackageName("com.hello.world.Class1$Inner1"));
    }

    public void testJarPathToClassName()
    {
        assertEquals("com.atlassian.osgi.Test", TransformStageUtils.jarPathToClassName("com/atlassian/osgi/Test.class"));
        assertEquals("Test", TransformStageUtils.jarPathToClassName("Test.class"));
        assertEquals(null, TransformStageUtils.jarPathToClassName("META-INF/atlassian-plugin.xml"));
        assertEquals(null, TransformStageUtils.jarPathToClassName(null));
        assertEquals(null, TransformStageUtils.jarPathToClassName(""));
    }
}
