package com.atlassian.plugin.cache.filecache.impl;


import com.atlassian.plugin.cache.filecache.FileCache;
import com.atlassian.plugin.cache.filecache.FileCacheKey;
import com.atlassian.plugin.cache.filecache.FileCacheStreamProvider;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.webresource.WebResourceIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This implementation does not remember its state across restarts.
 * <p/>
 * If a key is cached, calls to stream() will not block other calls trying to stream the same key, or other keys.
 * <p/>
 * If a key is not cached:
 * - calling stream() will not block the stream of other cached keys.
 * - it will block other calls to stream() for the same key (it waits for the file to be finished caching)
 * - a cache-size check is performed
 * <p/>
 * During a cache-size check:
 * - in-progress calls to stream() will not block
 * - new calls to stream() WILL block
 * - other cache-size checks will block
 * <p/>
 * <p/>
 *
 * @since 2.11.0
 */
public class LRUFileCache implements FileCache
{

    private static final String EXT = ".cachedfile";

    private final File tmpDir;
    private final int maxFiles;

    private final LinkedHashMap<FileCacheKey, CachedFile> nodes;
    private final Lock nodesLock = new ReentrantLock();
    private static final Logger log = LoggerFactory.getLogger(LRUFileCache.class);

    private final AtomicInteger filenameCounter = new AtomicInteger(0);

    /**
     * Sole constructor.
     *
     * @param tempDir provides information as to where to store cache files.
     * @param maxFiles               maximum number of files to retain. This class will try to honour this but at any point there may be
     *                               more or less files on disk, it is not an absolute number that will never be exceeded.
     * @throws IOException if the temp directory could not be created, or is not a directory.
     */
    public LRUFileCache(File tempDir, int maxFiles) throws IOException
    {
        this.tmpDir = tempDir;

        if (maxFiles < 1)
        {
            throw new IllegalArgumentException("Max files can not be less than one");
        }
        this.maxFiles = maxFiles;

        nodes = new LinkedHashMap<FileCacheKey, CachedFile>(16, 0.75f, true);

        if (!tmpDir.mkdirs())
        {
            //if the directory exists the above will return false, and if it does exist we must clear it as we do not allow for restarts
            if (tmpDir.exists() && tmpDir.isDirectory())
            {
                File[] files = tmpDir.listFiles();
                for (File f : files)
                {
                    if (!f.delete())
                    {
                        log.warn("Could not delete file: " + f.getAbsolutePath());
                    }
                }
            }
        }
        if (!tmpDir.isDirectory())
        {
            throw new IOException("Could not create tmp directory " + tmpDir);
        }

    }

    /**
     * Stream from file cache if we have it cached, otherwise create a new file and stream from it.
     *
     *
     * @param key   any non-null, non-empty string that identifies the cached item
     * @param dest  where to write the cached item to
     * @param input provides the underlying item on a cache-miss
     * @throws IOException if anything goes wrong and recovery is not possible during streaming.
     */
    public void stream(FileCacheKey key, OutputStream dest, FileCacheStreamProvider input) throws DownloadException
    {
        streamImpl(key, dest, input);
    }

    /**
     * Stream from file if exists, otherwise create file and stream from it.
     *
     * @return true if there was a cache-hit
     */
    boolean streamImpl(FileCacheKey key, OutputStream dest, FileCacheStreamProvider input) throws DownloadException
    {

        boolean newNode = false;

        CachedFile cachedFile = null;
        try
        {
                nodesLock.lock();
                try
                {
                    cachedFile = nodes.get(key);
                    if (cachedFile == null)
                    {
                        cachedFile = newNode();
                        nodes.put(key, cachedFile);
                        newNode = true;
                    }
                }
                finally
                {
                    nodesLock.unlock();
                }

                cachedFile.stream(dest, input);
        }
        catch (IOException e)
        {

            clean(key, newNode);
            new DownloadException(e);
        }
        catch (RuntimeException e)
        {
            clean(key, newNode);
            throw e;
        }
        finally
        {
            if (newNode)
            {
                nodesLock.lock();
                // update the size of the cache, and maybe evict some nodes
                try
                {
                    while (nodes.size() > maxFiles)
                    {
                        Iterator<Map.Entry<FileCacheKey, CachedFile>> iterator = nodes.entrySet().iterator();
                        Map.Entry<FileCacheKey, CachedFile> evictEntry = iterator.next();
                        iterator.remove();

                        CachedFile evictee = evictEntry.getValue();
                        evictee.delete();
                    }
                }
                finally
                {
                    nodesLock.unlock();
                }
            }
        }


        return !newNode;
    }

    private void clean(FileCacheKey key, boolean newNode)
    {
        if (newNode)
        {
            CachedFile item;
            nodesLock.lock();
            try
            {
                item = nodes.remove(key);
            }
            finally
            {
                nodesLock.unlock();
            }
            if (item != null)
            {
                try
                {
                    item.delete();
                }
                catch (DownloadException e)
                {
                    log.warn("Could not delete file ", e);
                }
            }
        }
    }

    private CachedFile newNode() throws IOException
    {
        int id = filenameCounter.incrementAndGet();
        File file = new File(tmpDir, id + EXT);
        return new CachedFile(file);
    }
}