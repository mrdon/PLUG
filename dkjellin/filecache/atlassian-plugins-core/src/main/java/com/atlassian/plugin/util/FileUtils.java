package com.atlassian.plugin.util;

import com.atlassian.plugin.util.zip.UrlUnzipper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils
{
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

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

    /**
     * If possible, return the File equivalent of a URL.
     * @return the file, or null if it does not represent a file
     */
    public static File toFile(final URL url)
    {
        if (!"file".equalsIgnoreCase(url.getProtocol())) {
            return null;
        }

        try
        {
            return new File(url.toURI());
        }
        catch (URISyntaxException e)
        {
            log.error("Could not convert file:// url to a File", e);
            return null;
        }
    }

    /**
     * @deprecated Since 2.0.0
     */
    public static void deleteDir(File directory)
    {
        try
        {
            org.apache.commons.io.FileUtils.deleteDirectory(directory);
        } catch (IOException e)
        {
            log.error("Unable to delete directory: "+directory.getAbsolutePath(), e);
        }
    }

}