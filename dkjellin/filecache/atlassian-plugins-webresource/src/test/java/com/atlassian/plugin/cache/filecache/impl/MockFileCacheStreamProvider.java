package com.atlassian.plugin.cache.filecache.impl;

import com.atlassian.plugin.cache.filecache.FileCacheStreamProvider;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Class to simplify testing the file caching.
 * @since 2.10.0
 */
class MockFileCacheStreamProvider implements FileCacheStreamProvider
{
    final byte[] bytes;
    boolean producedStream = false;

    MockFileCacheStreamProvider(byte[] bytes) {
        this.bytes = bytes;
    }

    public void writeStream(OutputStream dest) throws IOException {
        producedStream = true;
        dest.write(bytes);
    }
}