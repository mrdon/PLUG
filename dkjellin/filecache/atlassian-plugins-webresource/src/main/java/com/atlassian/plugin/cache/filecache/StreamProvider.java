package com.atlassian.plugin.cache.filecache;


import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface used by the file caching system. Items wishing to participate in participate in file cacheing will need to
 * implement this interface. This interface gives the file cache a means to get hold of the contents that will be cached.
 * @since 2.10.0
 */
public interface StreamProvider {
    /**
     * Produce the complete stream and write to the designated output stream. Classes implementing this method should not
     * close the output stream.
     * @param dest designated output stream.
     * @throws IOException If something goes awry while writing the file.
     */
    void produceStream(OutputStream dest) throws IOException;
}