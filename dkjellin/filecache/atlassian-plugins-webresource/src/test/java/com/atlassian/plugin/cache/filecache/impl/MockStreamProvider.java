package com.atlassian.plugin.cache.filecache.impl;

import com.atlassian.plugin.cache.filecache.StreamProvider;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Class to simplify testing the file caching.
 * @since 2.10.0
 */
class MockStreamProvider implements StreamProvider
{
    final byte[] bytes;
    boolean producedStream = false;

    MockStreamProvider(byte[] bytes) {
        this.bytes = bytes;
    }

    public void produceStream(OutputStream dest) throws IOException {
        producedStream = true;
        dest.write(bytes);
    }
}