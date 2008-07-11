package com.atlassian.plugin.osgi.loader.transform;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.test.PluginTestUtils;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.osgi.framework.Constants;
import org.dom4j.DocumentException;

public class TestDefaultPluginTransformer extends TestCase
{
    public void testGenerateManifest() throws URISyntaxException, IOException, PluginParseException
    {
        final File file = PluginTestUtils.getFileForResource("myapp-1.0-plugin.jar");

        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        byte[] manifest = transformer.generateManifest(getClassLoader(file).getResourceAsStream(PluginManager.PLUGIN_DESCRIPTOR_FILENAME), file);
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest));
        Attributes attrs = mf.getMainAttributes();

        assertEquals("1.1", attrs.getValue(Constants.BUNDLE_VERSION));
        assertEquals("com.atlassian.plugin.sample", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("This is a brief textual description of the plugin", attrs.getValue(Constants.BUNDLE_DESCRIPTION));
        assertEquals("Atlassian Software Systems Pty Ltd", attrs.getValue(Constants.BUNDLE_VENDOR));
        assertEquals("http://www.atlassian.com", attrs.getValue(Constants.BUNDLE_DOCURL));
        assertEquals("com.mycompany.myapp", attrs.getValue(Constants.EXPORT_PACKAGE));
        assertEquals(".", attrs.getValue(Constants.BUNDLE_CLASSPATH));
        assertEquals("*;create-asynchronously:=false", attrs.getValue("Spring-Context"));
        
    }

    public void testGenerateManifest_innerjars() throws URISyntaxException, PluginParseException, IOException
    {
        final File file = PluginTestUtils.getFileForResource(PluginTestUtils.SIMPLE_TEST_JAR);

        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        byte[] manifest = transformer.generateManifest(getClassLoader(file).getResourceAsStream(PluginManager.PLUGIN_DESCRIPTOR_FILENAME), file);
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest));
        Attributes attrs = mf.getMainAttributes();

        assertEquals(PluginTestUtils.PROJECT_VERSION, attrs.getValue(Constants.BUNDLE_VERSION));
        assertEquals("test.atlassian.plugin", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));

        final Collection classpathEntries = Arrays.asList(attrs.getValue(Constants.BUNDLE_CLASSPATH).split(","));
        assertEquals(3, classpathEntries.size());
        assertTrue(classpathEntries.contains("."));
        assertTrue(classpathEntries.contains("META-INF/lib/" + PluginTestUtils.INNER1_TEST_JAR));
        assertTrue(classpathEntries.contains("META-INF/lib/" + PluginTestUtils.INNER2_TEST_JAR));
    }

    public void testAddFilesToZip() throws URISyntaxException, IOException
    {
        final File file = PluginTestUtils.getFileForResource("myapp-1.0-plugin.jar");

        Map<String,byte[]> files = new HashMap<String, byte[]>() {{
            put("foo", "bar".getBytes());
        }};
        File copy = DefaultPluginTransformer.addFilesToExistingZip(file, files);
        assertNotNull(copy);
        assertTrue(!copy.getName().equals(file.getName()));
        assertTrue(copy.length() != file.length());

        ZipFile zip = new ZipFile(copy);
        ZipEntry entry = zip.getEntry("foo");
        assertNotNull(entry);
    }

    public void testGenerateSpringXml() throws Exception
    {
        // no components
        assertSpringTransformContains("<foo/>","</beans:beans>");

        // private component
        assertSpringTransformContains("<foo><component key='foo' class='my.Foo'/></foo>",
                                      "<beans:bean id='foo' class='my.Foo'/>");

        // public component without interface
        assertSpringTransformContains("<foo><component key='foo' class='my.Foo' public='true'/></foo>",
                                      "<beans:bean id='foo' class='my.Foo'",
                                      "<osgi:service id='foo' ref='foo'");

        // public component with interface
        assertSpringTransformContains("<foo><component key='foo' class='my.Foo' public='true'><interface>my.IFoo</interface></component></foo>",
                                      "<osgi:interfaces>",
                                      "<beans:value>my.IFoo</beans:value>");
    }

    public void testGenerateSpringXml_imports() throws Exception
    {
        // private component
        assertSpringTransformContains("<foo><component-import key='foo' interface='my.Foo'/></foo>",
                                      "<osgi:reference id='foo' interface='my.Foo'/>");

        // public component with interface
        assertSpringTransformContains("<foo><component-import key='foo'><interface>my.IFoo</interface></component-import></foo>",
                                      "<osgi:reference id='foo'><osgi:interfaces><beans:value>my.IFoo</beans:value></osgi:interfaces></osgi:reference>");

    }

    public void testGenerateSpringXml_hostComponents() throws Exception
    {
        // host componen with name
        assertSpringTransformContains(new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration("foo", String.class));
        }},
                "<foo/>",
                "<osgi:reference id='foo' context-class-loader='service-provider'",
                "<osgi:interfaces>",
                "<beans:value>java.lang.String</beans:value>",
                "filter='(bean-name=foo)");

        // host componen with name with # sign
        assertSpringTransformContains(new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration("foo#1", String.class));
        }},
                "<foo/>",
                "<osgi:reference id='fooLB1'",
                "<osgi:interfaces>",
                "<beans:value>java.lang.String</beans:value>",
                "filter='(bean-name=foo#1)");

        // host component with no bean name
        assertSpringTransformContains(new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration(String.class));
        }},
                "<foo/>",
                "<osgi:reference id='bean0'",
                "<osgi:interfaces>",
                "<beans:value>java.lang.String</beans:value>");

    }

    public void testTransform() throws URISyntaxException, IOException, PluginParseException, DocumentException
    {
        final File file = PluginTestUtils.getFileForResource("myapp-1.0-plugin2.jar");

        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        File copy = transformer.transform(file, new ArrayList<HostComponentRegistration>() {{
            add(new StubHostComponentRegistration(String.class));
        }});

        assertNotNull(copy);
        final JarFile jar = new JarFile(copy);
        final Attributes attrs = jar.getManifest().getMainAttributes();

        assertEquals("1.1", attrs.getValue(Constants.BUNDLE_VERSION));
        assertEquals("com.atlassian.plugin.sample", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("This is a brief textual description of the plugin", attrs.getValue(Constants.BUNDLE_DESCRIPTION));
        assertEquals("Atlassian Software Systems Pty Ltd", attrs.getValue(Constants.BUNDLE_VENDOR));
        assertEquals("http://www.atlassian.com", attrs.getValue(Constants.BUNDLE_DOCURL));
        assertEquals("com.mycompany.myapp", attrs.getValue(Constants.EXPORT_PACKAGE));

        assertNotNull(jar.getEntry(DefaultPluginTransformer.ATLASSIAN_PLUGIN_SPRING_XML));

    }

    private void assertSpringTransformContains(String input, String... outputs) throws DocumentException, IOException
    {
        assertSpringTransformContains(null, input, outputs);
    }

    private void assertSpringTransformContains(List<HostComponentRegistration> regs, String input, String... outputs) throws DocumentException, IOException
    {
        DefaultPluginTransformer trans = new DefaultPluginTransformer();
        String generated = new String(trans.generateSpringXml(stringToStream(input), regs)).replace('\"', '\'');
        for (String output : outputs)
        {
            boolean passed = generated.replaceAll("[ \\r\\n]", "").contains(output.replaceAll("[ \\r\\n]", ""));
            if (!passed)
                fail("Output "+generated+" does not contain "+output);
        }
    }

    private InputStream stringToStream(String value)
    {
        return new ByteArrayInputStream(value.getBytes());
    }

    private ClassLoader getClassLoader(File file) throws MalformedURLException
    {
        return new URLClassLoader(new URL[]{file.toURL()}, null);
    }

    static class StubHostComponentRegistration implements HostComponentRegistration
    {
        private String[] mainInterfaces;
        private Dictionary<String,String> properties;
        private Class[] mainInterfaceClasses;

        public StubHostComponentRegistration(Class... ifs)
        {
            this(null, ifs);
        }

        public StubHostComponentRegistration(String name, Class... ifs)
        {
            this.mainInterfaceClasses = ifs;
            mainInterfaces = new String[ifs.length];
            for (int x=0; x<ifs.length; x++)
                mainInterfaces[x] = ifs[x].getName();
            this.properties = new Hashtable<String,String>();
            if (name != null)
                properties.put("bean-name", name);
        }

        public StubHostComponentRegistration(String[] ifs, Dictionary<String,String> props)
        {
            mainInterfaces = ifs;
            this.properties = props;
        }

        public Object getInstance()
        {
            return null;
        }

        public Class[] getMainInterfaceClasses()
        {
            return mainInterfaceClasses;
        }

        public Dictionary<String, String> getProperties()
        {
            return properties;
        }

        public String[] getMainInterfaces()
        {
            return mainInterfaces;
        }
    }
}
