package com.atlassian.plugin.osgi.container.felix;

import com.atlassian.plugin.util.ClassLoaderStack;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TestExportsBuilderUtils extends TestCase
{
    public void testParseExportWithVersions() throws IOException
    {
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("com.atlassian.dummy", "1.2.3");
        expected.put("com.atlassian.dummy.sub", "4.5.6");
        expected.put("com.atlassian.wahaha", "1.3.6.SNAPSHOT");

        runParsingTest(expected, " # comment",
                                 "com.atlassian.dummy  =1.2.3",
                                 "com.atlassian.dummy.sub=  4.5.6",
                                 "com.atlassian.wahaha= 1.3.6-SNAPSHOT");
    }

    public void testParseExportNoVersions() throws IOException
    {
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("com.atlassian.dummy", null);
        expected.put("com.atlassian.dummy.sub", null);
        expected.put("com.atlassian.wahaha", null);

        runParsingTest(expected, " # comment",
                                 "com.atlassian.dummy",
                                 "com.atlassian.dummy.sub       ",
                                 "      com.atlassian.wahaha");
    }

    public void testConstructJdkExportsFromTestResource()
    {
        Map<String, String> content = ExportBuilderUtils.parseExportFile("jdk-packages.test.txt");

        Map<String, String> expected = new HashMap<String, String>();
        expected.put("foo.bar", null);
        expected.put("foo.baz", null);

        assertEquals(expected, content);
    }

    public void testConstructJdkExports()
    {
        Map<String, String> content = ExportBuilderUtils.parseExportFile(ExportsBuilder.JDK_PACKAGES_PATH);
        assertTrue(content.containsKey("org.xml.sax"));
    }

    public void runParsingTest(Map<String, String> expected, String... inputLines) throws IOException
    {
        String exportFile = createExportFile(inputLines);

        ClassLoaderStack.push(new MockedClassLoader(exportFile));
        Map<String, String> output;

        try
        {
            output = ExportBuilderUtils.parseExportFile(exportFile);
        }
        finally
        {
            ClassLoaderStack.pop();
        }

        assertEquals(expected, output);
    }

    private String createExportFile(String[] lines) throws IOException
    {
        File file = File.createTempFile("TestExportsBuilderUtils", "txt");
        file.deleteOnExit();

        PrintStream ps = null;
        try
        {
            ps = new PrintStream(file);

            for(String line:lines)
            {
                ps.println(line);
            }
        }
        finally
        {
            IOUtils.closeQuietly(ps);
        }

        return file.getAbsolutePath();
    }

    static class MockedClassLoader extends ClassLoader
    {
        private String filePath;

        MockedClassLoader(String filePath)
        {
            super(null);
            this.filePath = filePath;
        }

        @Override
        public URL getResource(String name)
        {
            try
            {
                return new URL("file://localhost" + filePath);
            }
            catch (MalformedURLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
