package com.atlassian.plugin.test;

import junit.framework.TestCase;

import java.io.File;
import java.net.URLClassLoader;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class PluginBuilderTest extends TestCase {

    public void testBuild() throws Exception {
        File jar = new PluginBuilder("foo")
                .addJava("my.Foo", "package my; public class Foo { public String hi() {return \"hi\";}}")
                .addResource("foo.txt", "Some text")
                .addPluginInformation("someKey", "someName", "1.33")
                .build();
        assertNotNull(jar);

        URLClassLoader cl = new URLClassLoader(new URL[]{jar.toURL()});
        Class cls = cl.loadClass("my.Foo");
        assertNotNull(cls);
        Object foo = cls.newInstance();
        String result = (String) cls.getMethod("hi").invoke(foo);
        assertEquals("hi", result);
        assertEquals("Some text", IOUtils.toString(cl.getResourceAsStream("foo.txt")));
        assertNotNull(cl.getResource("META-INF/MANIFEST.MF"));

        String xml = IOUtils.toString(cl.getResourceAsStream("atlassian-plugins.xml"));
        assertTrue(xml.contains("someKey"));
        assertTrue(xml.contains("someName"));
        assertTrue(xml.contains("1.33"));
    }
}
