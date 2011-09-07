package com.atlassian.plugin.cache.filecache.impl;


import com.atlassian.plugin.cache.filecache.StreamProvider;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Part of the implementation of LRUFileCache.
 * This class encapsulates the lifecycle of a cached file, from as-yet-uncached, to cached, to deleted.
 * <p/>
 * The stream() method is called to stream the cached file (or in the first instance, cache the underlying input).
 * <p/>
 * The stream() and delete() methods can be called in any combination.
 * <p/>
 * If the call to stream() worked, it will return some type of SUCCESS.
 * If the file has already been deleted, stream() will return TRY_AGAIN.
 * <p/>
 * This implementation will concurrently execute calls to stream().
 * A call to delete() will block untill all current calls to stream() have finished.
 * <p/>
 * After any call to stream(), fileSize() will return the size of the cached file. Before the first call to stream(),
 * this value is undefined.
 *
 * @since 2.10.0
 */
class CachedFile
{
    private static final Logger LOG = LoggerFactory.getLogger(CachedFile.class);

    static enum State
    {
        UNCREATED, EXISTS, EVICTED
    }

    static enum StreamResult
    {
        SUCCESS_CACHED, SUCCESS_CREATED, TRY_AGAIN
    }

    private final File file;
    private transient State state;

    /**
     * controls access to state
     */
    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Sole constructor.
     * @param file file to create/read when requested.
     */
    public CachedFile(File file)
    {
        this.file = file;
        if (file.exists())
        {
            this.state = State.EXISTS;
        }
        else
        {
            this.state = State.UNCREATED;
        }
    }

    /**
     * Stream the file to the destination, making use of the cached file if it exists.
     * @throws IOException on any error caching the file, or writing the file to dest
     */
    public StreamResult stream(OutputStream dest, StreamProvider input) throws IOException
    {
        boolean cacheHit = createIfNeeded(input);
        try
        {
            lock.readLock().lock();

            if (state == State.EVICTED)
            {
                return StreamResult.TRY_AGAIN;
            }

            FileInputStream cachein = new FileInputStream(file);
            IOUtils.copyLarge(cachein, dest);
            cachein.close();
        }
        finally
        {
            lock.readLock().unlock();
        }

        return cacheHit ? StreamResult.SUCCESS_CACHED : StreamResult.SUCCESS_CREATED;
    }


    /**
     * Delete the file and change state to EVICTED,
     */
    public void delete()
    {
        try
        {
            lock.writeLock().lock(); // no other readers! safe to delete
            state = State.EVICTED;

            if (file.exists())
            {
                if (!file.delete())
                {
                    LOG.warn("Could not delete cache file " + file);
                }
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    private boolean createIfNeeded(StreamProvider input) throws IOException
    {
        boolean cacheHit = true;
        lock.readLock().lock();
        try
        {
            if (state == State.EXISTS && !file.exists()) //file has been deleted outside of our control, we must re-create it
            {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try
                {
                    state = State.UNCREATED;
                }
                finally
                {
                    lock.writeLock().unlock();
                }
                lock.readLock().lock();//is this needed?
            }
        }
        finally
        {
            lock.readLock().unlock();
        }
        if (state == State.UNCREATED)
        {
            try
            {
                lock.writeLock().lock();
                if (state == State.UNCREATED)
                {
                    cacheHit = false;
                    streamToFile(input);
                    state = State.EXISTS;
                }
            }
            finally
            {
                lock.writeLock().unlock();
            }
        }
        return cacheHit;
    }

    private void streamToFile(StreamProvider input) throws IOException
    {
        OutputStream cacheout = null;
        try
        {
            cacheout = new BufferedOutputStream(new FileOutputStream(file));
            input.produceStream(cacheout);
        }
        finally
        {
            IOUtils.closeQuietly(cacheout);
        }
    }

}