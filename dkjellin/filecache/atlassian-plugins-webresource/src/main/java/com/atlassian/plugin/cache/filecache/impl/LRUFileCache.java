package com.atlassian.plugin.cache.filecache.impl;


import com.atlassian.plugin.cache.filecache.FileCache;
import com.atlassian.plugin.cache.filecache.StreamProvider;
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
 * @since 2.10.0
 */
public class LRUFileCache implements FileCache
{

    private static final String EXT = ".cachedfile";

    private final File tmpDir;
    private final int maxFiles;

    private final LinkedHashMap<String, CachedFile> nodes;
    private final Object nodesLock = new Object();
    private static final Logger log = LoggerFactory.getLogger(LRUFileCache.class);

    private final AtomicInteger filenameCounter = new AtomicInteger(0);

    /**
     * Sole constructor.
     * @param webResourceIntegration provides information as to where to store cache files.
     * @param maxFiles maximum number of files to retain. This class will try to honour this but at any point there may be
     * more or less files on disk, it is not an absolute number that will never be exceeded.
     * @throws IOException if the temp directory could not be created, or is not a directory.
     */
    public LRUFileCache(WebResourceIntegration webResourceIntegration, int maxFiles) throws IOException
    {
        this.tmpDir =webResourceIntegration.getTemporaryDirectory();

        if(maxFiles < 1)
        {
            throw new IllegalArgumentException("Max files can not be less than one");
        }
        this.maxFiles = maxFiles;

        nodes = new LinkedHashMap<String, CachedFile>(16, 0.75f, true);

        if(!tmpDir.mkdirs())
        {
            //if the directory exists the above will return false, and if it does exist we must clear it as we do not allow for restarts
            if(tmpDir.exists())
            {
                File[]  files = tmpDir.listFiles();
                if(files!=null)
                {
                    for(File f : files)
                    {
                        if(!f.delete())
                        {
                            log.warn("Could not delete file: " +f.getAbsolutePath());
                        }
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
     * @param key any non-null, non-empty string that identifies the cached item
     * @param dest where to write the cached item to
     * @param input provides the underlying item on a cache-miss
     * @throws IOException if anything goes wrong and recovery is not possible during streaming.
     */
    public void stream(String key, OutputStream dest, StreamProvider input) throws IOException
    {
        streamImpl(key, dest, input);
    }

    /**
     * Stream from file if exists, otherwise create file and stream from it.
     * @return true if there was a cache-hit
     */
    boolean streamImpl(String key, OutputStream dest, StreamProvider input) throws IOException
    {

        // putting things into and out of the nodes map is locked, and calling delete() on a node
        // is locked. But stream() is called outside of the lock and so can happen before/during/after
        // a call to delete(), and hence we loop until the stream it works.

        boolean newNode = false;

        CachedFile cachedFile = null;
        try
        {
            CachedFile.StreamResult result;
            do
            {

                synchronized (nodesLock)
                {
                    cachedFile = nodes.get(key);
                    if (cachedFile == null)
                    {
                        cachedFile = newNode();
                        nodes.put(key, cachedFile);
                        newNode = true;
                    }
                }

                result = cachedFile.stream(dest, input);


            }
            while (result == CachedFile.StreamResult.TRY_AGAIN);
        }
        catch (IOException e)
        {
            if (newNode)
            {
                clean(key);
            }
            throw e;
        }
        catch (RuntimeException e)
        {
            if (newNode)
            {
                clean(key);
            }
            throw e;
        }
        finally
        {
            if (newNode)
            {
                // update the size of the cache, and maybe evict some nodes
                synchronized (nodesLock)
                {
                    while (nodes.size() > maxFiles)
                    {
                        Iterator<Map.Entry<String, CachedFile>> iterator = nodes.entrySet().iterator();
                        Map.Entry<String, CachedFile> evictEntry = iterator.next();
                        iterator.remove();

                        CachedFile evictee = evictEntry.getValue();
                        evictee.delete();
                    }
                }
            }
        }


        return !newNode;
    }

    private void clean(String key)
    {
        CachedFile item;
        synchronized (nodesLock)
        {
            item = nodes.remove(key);
        }
        if(item!=null)
        {
            item.delete();
        }
    }

    private CachedFile newNode() throws IOException
    {
        int id = filenameCounter.incrementAndGet();
        File file = new File(tmpDir, id + EXT);
        if (file.exists())
        {
            // delete it, we own this directory
            if (!file.delete())
            {
                throw new IOException("Could not create file cache file because of undeletable existing file " + file.getAbsolutePath());
            }
        }
        return new CachedFile(file);
    }
}