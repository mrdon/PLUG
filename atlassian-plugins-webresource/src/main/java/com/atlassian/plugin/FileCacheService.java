package com.atlassian.plugin;

import java.io.File;

public interface FileCacheService
{
    /**
     * Returns the tempdir as specified by the host application. If the directory does not
     * exist an attmept to creaste it will be made.
     * @return
     */
    File getTempDir();

    /**
     * Returns a reference to a file held by this cache. If this cache does not
     * hold a File for the hash provided it will return null.
     * @param hash hash file is stored under.
     * @return a reference to the File, or null if this file is not held by the cache.
     */
    File getFile(String hash);

    /**
     * Add a new file to the cache.
     * @param hash hash to store the file under.
     * @param file reference to the file to store
     */
    void putFile(String hash, File file);

    /**
     * Convinence method to get the locale for static resources as defined by
     * the WebResourceIntegration interface.
     * @return
     */
    String getStaticResourceLocale();
}
