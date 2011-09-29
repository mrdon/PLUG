package com.atlassian.plugin.cache.filecache;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @since 2.10
 */
public interface FileCache {

    /**
     * Stream the contents identified by the key to the destination stream. Should the contents not exist in the cache
     * a new entry should be created if the implementation is a caching implementation.
     * @param key can not be null
     * @param dest where to write the cached item to
     * @param input provides the underlying item on a cache-miss
     * @throws IOException if there was an error writing to dest, or reading from input, or reading from the cache
     */
    void stream(FileCacheKey key, OutputStream dest, FileCacheStreamProvider input) throws IOException;

}