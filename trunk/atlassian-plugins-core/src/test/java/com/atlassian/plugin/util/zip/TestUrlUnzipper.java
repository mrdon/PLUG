package com.atlassian.plugin.util.zip;

import com.atlassian.plugin.test.PluginTestUtils;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TestUrlUnzipper extends TestCase
{

    private File basedir;
    private File destdir;
    private File zip;
    private File sourcedir;
    private File source1;

    @Override
    protected void setUp() throws Exception {
        basedir = PluginTestUtils.createTempDirectory(TestUrlUnzipper.class);
        FileUtils.cleanDirectory(basedir);
        destdir = new File(basedir, "dest");
        destdir.mkdir();
        zip = new File(basedir, "test.zip");
        sourcedir = new File(basedir, "source");
        sourcedir.mkdir();
        source1 = new File(sourcedir, "source1.jar");
        FileUtils.writeStringToFile(source1, "source1");
        source1.setLastModified(source1.lastModified() - 100000);
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testConditionalUnzip() throws IOException, InterruptedException {
        zip(sourcedir, zip);
        UrlUnzipper unzipper = new UrlUnzipper(zip.toURL(), destdir);
        unzipper.conditionalUnzip();

        assertEquals(1, destdir.listFiles().length);
        File dest1 = destdir.listFiles()[0];
        assertEquals(source1.lastModified(), dest1.lastModified());
        assertEquals("source1", FileUtils.readFileToString(dest1));

        FileUtils.writeStringToFile(source1, "source1-modified");
        zip(sourcedir, zip);
        unzipper = new UrlUnzipper(zip.toURL(), destdir);
        unzipper.conditionalUnzip();

        assertEquals(1, destdir.listFiles().length);
        dest1 = destdir.listFiles()[0];
        assertEquals(source1.lastModified(), dest1.lastModified());
        assertEquals("source1-modified", FileUtils.readFileToString(dest1));

    }

    public void testConditionalUnzipCleansZipDirectory() throws IOException, InterruptedException
    {
        File f = new File(destdir, "bogusfile.txt");
        zip(sourcedir, zip);
        UrlUnzipper unzipper = new UrlUnzipper(zip.toURL(), destdir);
        unzipper.conditionalUnzip();
        assertEquals(1, destdir.listFiles().length); // there's only one..
        assertEquals("source1.jar", destdir.listFiles()[0].getName()); // ..and it's not randomfile.txt
    }

    public void testConditionalUnzipWithNoUnzipIfNoFileMod() throws IOException, InterruptedException {
        zip(sourcedir, zip);
        UrlUnzipper unzipper = new UrlUnzipper(zip.toURL(), destdir);
        unzipper.conditionalUnzip();

        assertEquals(1, destdir.listFiles().length);
        File dest1 = destdir.listFiles()[0];
        assertEquals(source1.lastModified(), dest1.lastModified());
        assertEquals("source1", FileUtils.readFileToString(dest1));

        long ts = source1.lastModified();
        FileUtils.writeStringToFile(source1, "source1-modified");
        source1.setLastModified(ts);
        zip(sourcedir, zip);
        unzipper = new UrlUnzipper(zip.toURL(), destdir);
        unzipper.conditionalUnzip();

        assertEquals(1, destdir.listFiles().length);
        dest1 = destdir.listFiles()[0];
        assertEquals(source1.lastModified(), dest1.lastModified());
        assertEquals("source1", FileUtils.readFileToString(dest1));

    }

    private void zip(File basedir, File destfile) throws IOException {
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(destfile));
        for (File child : basedir.listFiles())
        {
            ZipEntry entry = new ZipEntry(child.getName());
            entry.setTime(child.lastModified());
            zout.putNextEntry(entry);
            FileInputStream input = new FileInputStream(child);
            IOUtils.copy(input, zout);
            input.close();

            // not sure why this is necessary...
            child.setLastModified(entry.getTime());
        }
        zout.close();
    }
}
