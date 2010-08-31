package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.test.PluginJarBuilder;
import com.google.common.collect.Sets;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;
import java.util.jar.JarInputStream;

public class TestTransformStageUtils extends TestCase
{
    public void testScanJarForItems() throws Exception
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

        FileInputStream fis = new FileInputStream(pluginFile);
        JarInputStream jis = new JarInputStream(fis);
        Set<String> classes = TransformStageUtils.scanJarForItems(jis, Sets.newHashSet("my.Foo"), TransformStageUtils.JarEntryToClassName.INSTANCE);

        assertEquals(1, classes.size());
        assertTrue(classes.contains("my.Foo"));
    }

    public void testFindClassesAvailableInInnerJars() throws Exception
    {
        // create inner jars.
        final File innerjar1 = new PluginJarBuilder("innerjar1")
                                    .addFormattedJava("my.innerpackage1.Interface1",
                                            "package my.innerpackage1;",
                                            "public class Interface1 {}")
                                    .addResource("META-INF/atlassian-plugin.xml", "helloworld")
                                    .build();

        final File innerjar2 = new PluginJarBuilder("innerjar2")
                                    .addFormattedJava("my.innerpackage2.Interface2",
                                            "package my.innerpackage2;",
                                            "public class Interface2 {}")
                                    .build();

        // create a jar with embedded jars.
        final File pluginJar = new PluginJarBuilder("plugin")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test Bundle instruction plugin 2' key='test.plugin'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions>",
                    "            <Export-Package>!*.internal.*,*</Export-Package>",
                    "        </bundle-instructions>",
                    "    </plugin-info>",
                    "</atlassian-plugin>")
                .addFormattedJava("my.MyFooChild",
                        "package my;",
                        "public class MyFooChild extends com.atlassian.plugin.osgi.factory.transform.dummypackage2.DummyClass2 {",
                        "}")
                .addFile("META-INF/lib/myjar1.jar", innerjar1)
                .addFile("META-INF/lib/myjar2.jar", innerjar2)
                .build();

        Set<String> found
                = TransformStageUtils.scanInnerJars(pluginJar,
                                                    Sets.newHashSet("META-INF/lib/myjar1.jar", "META-INF/lib/myjar2.jar"),
                                                    Sets.newHashSet("my.innerpackage1.Interface1", "my.innerpackage2.Interface2", "my.innerpackage3.Interface3"));
        assertEquals(2, found.size());
        assertTrue(found.contains("my.innerpackage1.Interface1"));
        assertTrue(found.contains("my.innerpackage2.Interface2"));
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
