package com.atlassian.plugin.util;

import java.io.*;

public class FileUtils
{
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private FileUtils()
    {
        //utility class should not be instantiated
    }

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
}