package com.atlassian.plugin.util.zip;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public abstract class AbstractUnzipper implements Unzipper
{
    protected static Log log = LogFactory.getLog(FileUnzipper.class);
    protected File destDir;

    protected File saveEntry(InputStream is, ZipEntry entry) throws IOException
    {
        File file = new File(destDir, entry.getName());

        if (entry.isDirectory())
        {
            file.mkdirs();
        }
        else
        {
            File dir = new File(file.getParent());
            dir.mkdirs();

            FileOutputStream fos = null;
            try
            {
                fos = new FileOutputStream(file);
                IOUtils.copy(is, fos);
                fos.flush();
            }
            catch (FileNotFoundException fnfe)
            {
                log.error("Error extracting a file to '" + destDir + File.separator + entry.getName() + "'. This destination is invalid for writing an extracted file stream to. ");
                return null;
            }
            finally
            {
                IOUtils.closeQuietly(fos);
            }
        }

        return file;
    }

    protected ZipEntry[] entries(ZipInputStream zis) throws IOException
    {
        List entries = new ArrayList();
        try
        {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null)
            {
                entries.add(zipEntry);
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        }
        finally
        {
            IOUtils.closeQuietly(zis);
        }

        return (ZipEntry[]) entries.toArray(new ZipEntry[entries.size()]);
    }

    public void conditionalUnzip() throws IOException
    {
        List zipContents = new ArrayList();

        ZipEntry[] zipEntries = entries();
        for (int i = 0; i < zipEntries.length; i++)
        {
            zipContents.add(zipEntries[i].getName());
        }

        // If the jar contents of the directory does not match the contents of the zip
        // The we will nuke the bundled plugins directory and re-extract.
        List targetDirContents = getContentsOfTargetDir(destDir);
        if (!targetDirContents.equals(zipContents))
        {
            FileUtils.deleteDirectory(destDir);
            unzip();
        }
        else
        {
            if (log.isDebugEnabled())
                log.debug("Target directory contents match zip contents. Do nothing.");
        }
    }

    protected List getContentsOfTargetDir(File dir)
    {
        // Create filter that lists only jars
        FilenameFilter filter = new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".jar");
            }
        };

        String[] children = dir.list(filter);

        if (children == null)
        {
            // No files, return empty array
            return Collections.emptyList();
        }

        ArrayList targetDirContents = new ArrayList();

        if (log.isDebugEnabled() && children.length > 0)
            log.debug("Listing JAR files in " + dir.getAbsolutePath());

        for (int i = 0; i < children.length; i++)
        {
            if (log.isDebugEnabled())
                log.debug(children[i]);
            targetDirContents.add(children[i]);

        }
        return targetDirContents;
    }
}
