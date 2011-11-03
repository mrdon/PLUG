package com.atlassian.plugin.cache.filecache.impl;


import com.atlassian.plugin.cache.filecache.FileCacheStreamProvider;
import com.atlassian.plugin.servlet.DownloadException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
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
 * <p/>
 * This implementation will concurrently execute calls to stream().
 * A call to delete() will block untill all current calls to stream() have finished.
 * <p/>
 * After any call to stream(), fileSize() will return the size of the cached file. Before the first call to stream(),
 * this value is undefined.
 *
 * @since 2.11.0
 */
class CachedFile
{
    private static final Logger LOG = LoggerFactory.getLogger(CachedFile.class);


    static enum StreamResult
    {
        SUCCESS_CACHED, SUCCESS_CREATED
    }

    private final File file;
//    private transient State state;

    /**
     * controls access to file
     */
    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Sole constructor.
     * @param file file to create/read when requested.
     */
    public CachedFile(File file) throws IOException
    {
        this.file = file;
        if (file.exists())
        {
            // delete it, we own this directory
            if (!file.delete())
            {
                throw new IOException("Could not create file cache file because of undeletable existing file " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Stream the file to the destination, making use of the cached file if it exists.
     * @throws IOException on any error caching the file, or writing the file to dest
     */
    public StreamResult stream(OutputStream dest, FileCacheStreamProvider input) throws DownloadException
    {
        boolean cacheHit = true;

            Lock currentLock;
            lock.readLock().lock();
            currentLock = lock.readLock();

            try
            {
                if (!file.exists()) //file has been deleted, we need to re-create it
                {
                    lock.readLock().unlock();
                    lock.writeLock().lock();
                    currentLock = lock.writeLock();

                    if (!file.exists()) //someone else may have written the file while we waited for the writelock..
                    {
                        cacheHit = false;
                        streamToFile(input);
                    }
                    lock.readLock().lock(); //downgrading the lock
                    lock.writeLock().unlock();
                    currentLock = lock.readLock();
                    streamToDestination(dest);
                }
                else
                {
                    streamToDestination(dest);
                }
            }
            finally
            {
                currentLock.unlock();
            }

        return cacheHit ? StreamResult.SUCCESS_CACHED : StreamResult.SUCCESS_CREATED;
    }


    /**
     * Delete the file, will block until all current reads are completed.
     */
    public void delete() throws DownloadException
    {
        try
        {
            lock.writeLock().lock(); // no other readers! safe to delete

            if (file.exists())
            {
                if (!file.delete())
                {
                    throw new DownloadException("Could not delete cache file " + file);
                }
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }


    private void streamToFile(FileCacheStreamProvider input) throws DownloadException
    {
        OutputStream cacheout = null;
        try
        {
            cacheout = new BufferedOutputStream(new FileOutputStream(file));
            input.writeStream(cacheout);
        }
        catch(IOException e)
        {
            throw new DownloadException(e);
        }
        finally
        {
            IOUtils.closeQuietly(cacheout);
        }
    }

    /**
     * Copies the in stream to the out stream and closes the in stream when done.
     * @param out destination stream
     * @throws IOException should any errors occur.
     */
    private void streamToDestination(OutputStream out) throws  DownloadException
    {
        InputStream in = null;
        try
        {
            in = new FileInputStream(file);
            IOUtils.copyLarge(in, out);
            out.flush();
        }
        catch(IOException e)
        {
            throw new DownloadException(e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

}