package com.atlassian.plugin.osgi;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;

import java.io.File;

public class TestLogging extends PluginInContainerTestBase
{
    public void testToStringPriming() throws Exception
    {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        final File pluginJar = new PluginJarBuilder("testUpgradeOfBundledPlugin")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.bundled.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='obj' class='my.Foo'/>",
                        "</atlassian-plugin>")
                .addFormattedJava("my.Foo",
                        "package my;",
                        "import com.atlassian.plugin.osgi.TestLogging;",
                        "public class Foo {",
                        "  public Foo() {",
                        "     throw new TestLogging.CountingException();",
                        "  }",
                        "}")
                .build();
        initBundlingPluginManager(factory, pluginJar);

        // for some reason, it is called 14 times w/o priming, but 19 with.  Sadly, this test only works correctly
        // when invoked from Maven
        assertEquals(19, CountingException.count);
    }

    public static class CountingException extends RuntimeException
    {
        public static int count = 0;
        public String toString()
        {
            count++;
            return "count:"+count;
        }
    }
}
