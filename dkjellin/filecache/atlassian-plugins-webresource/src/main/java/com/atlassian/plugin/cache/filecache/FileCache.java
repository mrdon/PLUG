package com.atlassian.plugin.cache.filecache;

import java.io.IOException;
import java.io.OutputStream;

/**
 */
public interface FileCache {

    /**
     *
     * @param key any non-null, non-empty string that identifies the cached item
     * @param dest where to write the cached item to
     * @param input provides the underlying item on a cache-miss
     * @throws IOException if there was an error writing to dest, or reading from input, or reading from the cache
     */
    void stream(String key, OutputStream dest, StreamProvider input) throws IOException;

}