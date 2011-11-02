package com.atlassian.plugin.cache.filecache.impl;

import com.atlassian.plugin.cache.filecache.FileCache;
import com.atlassian.plugin.cache.filecache.FileCacheKey;
import com.atlassian.plugin.cache.filecache.FileCacheStreamProvider;
import com.atlassian.plugin.servlet.DownloadException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is a pass through implementaiton of the filecache.
 * @since 2.11.0
 */
public class NonCachingFileCache implements FileCache
{
    /**
     * Will always all the stream provider for the contents of the stream and write it to the output.
     * @param key can not be null
     * @param dest where to write the cached item to
     * @param input provides the underlying item on a cache-miss
     * @throws IOException
     */
    public void stream(FileCacheKey key, OutputStream dest, FileCacheStreamProvider input) throws DownloadException
    {
        input.writeStream(dest);
    }
}
