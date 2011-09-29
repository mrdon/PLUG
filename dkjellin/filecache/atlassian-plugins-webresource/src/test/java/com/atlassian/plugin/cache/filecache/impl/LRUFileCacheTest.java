package com.atlassian.plugin.cache.filecache.impl;

import com.atlassian.plugin.cache.filecache.FileCacheKey;
import com.atlassian.plugin.webresource.WebResourceIntegration;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class LRUFileCacheTest extends TestCase {

    private WebResourceIntegration mockWebResourceIntegration =  mock(WebResourceIntegration.class);

    @Override
    public void setUp() throws Exception {
         when(mockWebResourceIntegration.getTemporaryDirectory()).thenReturn(new File(System.getProperty("java.io.tmpdir"),"test"));
    }

    @Override
    public void tearDown() throws Exception
    {
        mockWebResourceIntegration = null;
        super.tearDown();
    }

    public void testSizeLRU() throws Exception {
        LRUFileCache cache = new LRUFileCache(mockWebResourceIntegration, 2);

        byte[] afile = "this is the first one".getBytes();
        byte[] bfile = "this is the other one".getBytes();
        byte[] cfile = "this is yet another one".getBytes();

        cache(cache, new MockCacheKey("a"), afile, false);
        cache(cache, new MockCacheKey("a"), afile, true);

        cache(cache, new MockCacheKey("b"), bfile, false);
        cache(cache, new MockCacheKey("b"), bfile, true);

        cache(cache, new MockCacheKey("a"), afile, true); // still a hit

        cache(cache, new MockCacheKey("c"), cfile, false);
        cache(cache, new MockCacheKey("c"), cfile, true);

        cache(cache, new MockCacheKey("a"), afile, true); // still a hit

        cache(cache, new MockCacheKey("b"), bfile, false); // but a miss, evicted
        cache(cache, new MockCacheKey("b"), bfile, true);
        cache(cache, new MockCacheKey("c"), cfile, false); // but c should have fallen out


    }



    private void cache(LRUFileCache cache, FileCacheKey key, byte[] bytes, boolean hitExpected) throws Exception {
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        MockFileCacheStreamProvider input = new MockFileCacheStreamProvider(bytes);
        boolean hit = cache.streamImpl(key, dest, input);
        assertEquals(hitExpected, hit);
        assertEquals(!hitExpected, input.producedStream);
        byte[] actualBytes = dest.toByteArray();
        assertEquals(bytes.length, actualBytes.length);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], actualBytes[i]);
        }
    }

    private static class MockCacheKey implements FileCacheKey
    {
        final String key;

        private MockCacheKey(String key)
        {
            this.key = key;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MockCacheKey that = (MockCacheKey) o;

            if (key != null ? !key.equals(that.key) : that.key != null) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            return key != null ? key.hashCode() : 0;
        }
    }

}