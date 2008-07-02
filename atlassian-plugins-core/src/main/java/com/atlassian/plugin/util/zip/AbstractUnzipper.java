package com.atlassian.plugin.util.zip;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.List;
import java.util.ArrayList;

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
}
