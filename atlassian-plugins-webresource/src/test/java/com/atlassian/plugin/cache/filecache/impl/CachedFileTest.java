package com.atlassian.plugin.cache.filecache.impl;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class CachedFileTest extends TestCase {
    private File tmp;

    @Override
    protected void setUp() throws Exception {
        tmp = File.createTempFile("LRUFileCacheTest", null);
        tmp.delete();

    }

    public void testNormalLifecycle() throws Exception {
        CachedFile f = new CachedFile(tmp);

        assertFalse(tmp.exists());

        assertEquals(CachedFile.StreamResult.SUCCESS_CREATED, stream(f, true));
        assertTrue(tmp.exists());
        assertEquals(CachedFile.StreamResult.SUCCESS_CACHED, stream(f, false));
        assertEquals(CachedFile.StreamResult.SUCCESS_CACHED, stream(f, false));

        f.delete();
        assertFalse(tmp.exists());
        assertEquals(CachedFile.StreamResult.SUCCESS_CREATED, stream(f, true));
        assertEquals(CachedFile.StreamResult.SUCCESS_CACHED, stream(f, false));

    }

    private CachedFile.StreamResult stream(CachedFile f, boolean streamCalled) throws Exception {
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        MockFileCacheStreamProvider provider = new MockFileCacheStreamProvider(new byte[]{1, 2, 3});
        CachedFile.StreamResult streamResult = f.stream(dest, provider);
        assertEquals(streamCalled, provider.producedStream);
        return streamResult;
    }
}