package com.atlassian.plugin.cache.filecache.impl;

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

        cache(cache, "a", afile, false);
        cache(cache, "a", afile, true);

        cache(cache, "b", bfile, false);
        cache(cache, "b", bfile, true);

        cache(cache, "a", afile, true); // still a hit

        cache(cache, "c", cfile, false);
        cache(cache, "c", cfile, true);

        cache(cache, "a", afile, true); // still a hit

        cache(cache, "b", bfile, false); // but a miss, evicted
        cache(cache, "b", bfile, true);
        cache(cache, "c", cfile, false); // but c should have fallen out


    }



    private void cache(LRUFileCache cache, String key, byte[] bytes, boolean hitExpected) throws Exception {
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        MockStreamProvider input = new MockStreamProvider(bytes);
        boolean hit = cache.streamImpl(key, dest, input);
        assertEquals(hitExpected, hit);
        assertEquals(!hitExpected, input.producedStream);
        byte[] actualBytes = dest.toByteArray();
        assertEquals(bytes.length, actualBytes.length);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], actualBytes[i]);
        }
    }

}