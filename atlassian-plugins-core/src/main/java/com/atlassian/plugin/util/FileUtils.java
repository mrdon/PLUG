package com.atlassian.plugin.util;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.*;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.net.URL;

import com.atlassian.plugin.util.zip.UrlUnzipper;

public class FileUtils
{
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static final Log log = LogFactory.getLog(FileUtils.class);

    /**
     * Copy file from source to destination. The directories up to <code>destination</code> will be
     * created if they don't already exist. <code>destination</code> will be overwritten if it
     * already exists.
     *
     * @param source An existing non-directory <code>File</code> to copy bytes from.
     * @param destination A non-directory <code>File</code> to write bytes to (possibly
     * overwriting).
     *
     * @throws java.io.IOException if <code>source</code> does not exist, <code>destination</code> cannot be
     * written to, or an IO error occurs during copying.
     *
     */
    public static void copyFile(final File source, final File destination)
            throws IOException
    {
        //check source exists
        if (!source.exists())
        {
            final String message = "File " + source + " does not exist";
            throw new IOException(message);
        }

        //does destinations directory exist ?
        if (destination.getParentFile() != null &&
                !destination.getParentFile().exists())
        {
            destination.getParentFile().mkdirs();
        }

        //make sure we can write to destination
        if (destination.exists() && !destination.canWrite())
        {
            final String message = "Unable to open file " +
                    destination + " for writing.";
            throw new IOException(message);
        }

        final FileInputStream input = new FileInputStream(source);
        final FileOutputStream output = new FileOutputStream(destination);
        copy(input, output);
        shutdownStream(input);
        shutdownStream(output);

        if (source.length() != destination.length())
        {
            final String message = "Failed to copy full contents from " + source +
                    " to " + destination;
            throw new IOException(message);
        }
    }

    /**
     * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws IOException In case of an I/O problem
     */
    public static int copy(final InputStream input, final OutputStream output)
            throws IOException
    {
        return copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @param bufferSize Size of internal buffer to use.
     * @return the number of bytes copied
     * @throws IOException In case of an I/O problem
     */
    public static int copy(final InputStream input,
                           final OutputStream output,
                           final int bufferSize)
            throws IOException
    {
        final byte[] buffer = new byte[bufferSize];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer)))
        {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Unconditionally close an <code>OutputStream</code>.
     * Equivalent to {@link OutputStream#close()}, except any exceptions will be ignored.
     * @param output A (possibly null) OutputStream
     */
    public static void shutdownStream(final OutputStream output)
    {
        if (output == null)
        {
            return;
        }

        try
        {
            output.close();
        }
        catch (final IOException ioe)
        {
        }
    }

    /**
     * Unconditionally close an <code>InputStream</code>.
     * Equivalent to {@link InputStream#close()}, except any exceptions will be ignored.
     * @param input A (possibly null) InputStream
     */
    public static void shutdownStream(final InputStream input)
    {
        if (input == null)
        {
            return;
        }

        try
        {
            input.close();
        }
        catch (final IOException ioe)
        {
        }
    }

    /**
     *
     * @param srcDir
     * @param destDir
     * @throws IOException
     * @throws IllegalArgumentException if the <code>srcDir</code> does not exist
     */
    public static void copyDirectory(File srcDir, File destDir) throws IOException
    {
        if (!srcDir.exists())
            throw new IllegalArgumentException("Source dir [" + srcDir + "] does not exist");

        copyDirectory(srcDir, destDir, false);
    }

    /**
     *
      * @param srcDir
     * @param destDir
     * @param overwrite
     * @throws IOException
     */
    public static void copyDirectory(File srcDir, File destDir, boolean overwrite) throws IOException
    {
        File[] files = srcDir.listFiles();

        if (!destDir.exists())
            destDir.mkdirs();
        else
            log.debug(destDir.getAbsolutePath() + " already exists");

        if (files != null)
        {
            for (int i = 0; i < files.length; i++)
            {
                File file = files[i];
                File dest = new File(destDir, file.getName());

                if (file.isFile())
                    copyFile(file, dest);
                else
                    copyDirectory(file, dest, overwrite);
            }
        }
    }

    /**
     * safely performs a recursive delete on a directory
     *
     * @param dir
     * @return
     */
    public static boolean deleteDir(File dir)
    {
        if (dir == null)
        {
            return false;
        }

        // now we go through all of the files and subdirectories in the
        // directory and delete them one by one
        File[] files = dir.listFiles();
        if (files != null)
        {
            for (int i = 0; i < files.length; i++)
            {
                File file = files[i];

                // in case this directory is actually a symbolic link, or it's
                // empty, we want to try to delete the link before we try
                // anything
                boolean noDeleted = !file.delete();
                if (noDeleted)
                {
                    // deleting the file failed, so maybe it's a non-empty
                    // directory
                    if (file.isDirectory()) deleteDir(file);

                    // otherwise, there's nothing else we can do
                }
            }
        }

        // now that we tried to clear the directory out, we can try to delete it
        // again
        return dir.delete();
    }

    /**
     * Extract the zip from the URL into the destination directory, but only if the contents haven't already been
     * unzipped.  If the directory contains different contents than the zip, the directory is cleaned out
     * and the files are unzipped.
     *
     * @param zipUrl The zip url
     * @param destDir The destination directory for the zip contents
     */
    public static void conditionallyExtractZipFile(URL zipUrl, File destDir)
    {
        try
        {
            UrlUnzipper unzipper = new UrlUnzipper(zipUrl, destDir);
            unzipper.conditionalUnzip();
        }
        catch (IOException e)
        {
            log.error("Found " + zipUrl + ", but failed to read file", e);
        }
    }

}