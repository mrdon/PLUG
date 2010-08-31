package com.atlassian.plugin.util;

import com.atlassian.plugin.test.PluginJarBuilder;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

/**
 *
 */
public class TestChainingClassLoader extends TestCase
{
    public void testLoadClassInFirst() throws Exception
    {
        ClassLoader cl1 = new PluginJarBuilder().
                addFormattedJava("my.Foo", "package my; public class Foo {}").
                getClassLoader();
        ClassLoader cl2 = new PluginJarBuilder().
                getClassLoader();
        ChainingClassLoader ccl = new ChainingClassLoader(cl1, cl2);
        assertEquals(cl1, ccl.loadClass("my.Foo").getClassLoader());
    }

    public void testLoadClassInSecond() throws Exception
    {
        ClassLoader cl1 = new PluginJarBuilder().
                getClassLoader();
        ClassLoader cl2 = new PluginJarBuilder().
                addFormattedJava("my.Foo", "package my; public class Foo {}").
                getClassLoader();
        ChainingClassLoader ccl = new ChainingClassLoader(cl1, cl2);
        assertEquals(cl2, ccl.loadClass("my.Foo").getClassLoader());
    }

    public void testLoadClassInFirstHidesSecond() throws Exception
    {
        ClassLoader cl1 = new PluginJarBuilder().
                addFormattedJava("my.Foo", "package my; public class Foo {}").
                getClassLoader();
        ClassLoader cl2 = new PluginJarBuilder().
                addFormattedJava("my.Foo", "package my; public class Foo {}").
                getClassLoader();
        ChainingClassLoader ccl = new ChainingClassLoader(cl1, cl2);
        assertEquals(cl1, ccl.loadClass("my.Foo").getClassLoader());
    }

    public void testLoadResourceInFirst() throws Exception
    {
        ClassLoader cl1 = buildClassLoaderWithResources(new HashMap<String,String>() {{
            put("my/foo.txt", "foo");
        }});
        ClassLoader cl2 = buildClassLoaderWithResources(new HashMap<String,String>() {{
        }});
        ChainingClassLoader ccl = new ChainingClassLoader(cl1, cl2);
        assertEquals("foo", IOUtils.readLines(ccl.getResourceAsStream("my/foo.txt")).get(0));
    }

    public void testLoadResourceInSecond() throws Exception
    {
        ClassLoader cl1 = buildClassLoaderWithResources(new HashMap<String,String>() {{
        }});
        ClassLoader cl2 = buildClassLoaderWithResources(new HashMap<String,String>() {{
            put("my/foo.txt", "foo");
        }});
        ChainingClassLoader ccl = new ChainingClassLoader(cl1, cl2);
        assertEquals("foo", IOUtils.readLines(ccl.getResourceAsStream("my/foo.txt")).get(0));
    }

    public void testLoadResourceInFirstHidesSecond() throws Exception
    {
        ClassLoader cl1 = buildClassLoaderWithResources(new HashMap<String,String>() {{
            put("my/foo.txt", "foo");
        }});
        ClassLoader cl2 = buildClassLoaderWithResources(new HashMap<String,String>() {{
            put("my/foo.txt", "bar");
        }});
        ChainingClassLoader ccl = new ChainingClassLoader(cl1, cl2);
        assertEquals("foo", IOUtils.readLines(ccl.getResourceAsStream("my/foo.txt")).get(0));
    }

    public void testLoadResources() throws Exception
    {
        ClassLoader cl1 = buildClassLoaderWithResources(new HashMap<String,String>() {{
            put("my/foo.txt", "foo");
        }});
        ClassLoader cl2 = buildClassLoaderWithResources(new HashMap<String,String>() {{
            put("my/foo.txt", "bar");
        }});
        ChainingClassLoader ccl = new ChainingClassLoader(cl1, cl2);
        Enumeration<URL> e = ccl.getResources("my/foo.txt");
        assertEquals("foo", IOUtils.readLines(e.nextElement().openStream()).get(0));
        assertEquals("bar", IOUtils.readLines(e.nextElement().openStream()).get(0));
    }

    public void testLoadResourceWithNameOverride() throws Exception
    {
        ClassLoader cl1 = buildClassLoaderWithResources(new HashMap<String,String>() {{
            put("my/foo.txt", "foo");
            put("my/bar.txt", "bar");
        }});
        ClassLoader cl2 = buildClassLoaderWithResources(new HashMap<String,String>() {{
        }});
        ChainingClassLoader ccl = new ChainingClassLoader(singletonMap("my/bar.txt", "my/foo.txt"), cl1, cl2);
        assertEquals("foo", IOUtils.readLines(ccl.getResourceAsStream("my/bar.txt")).get(0));
        assertEquals("foo", IOUtils.readLines(ccl.getResourceAsStream("my/foo.txt")).get(0));
    }

    private URLClassLoader buildClassLoaderWithResources(Map<String,String> files)
            throws IOException
    {
        PluginJarBuilder builder = new PluginJarBuilder();
        for (Map.Entry<String,String> entry : files.entrySet())
        {
            builder.addResource(entry.getKey(), entry.getValue());
        }
        return new URLClassLoader(new URL[] {builder.build().toURI().toURL()}, null);
    }
}
