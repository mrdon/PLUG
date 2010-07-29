package com.atlassian.plugin.metadata;

import static org.apache.commons.io.IOUtils.readLines;

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.io.IOUtils;

/**
 * Utility for managing a using and closing a {@link Reader}.
 * 
 * @since 2.6
 */
final class ReaderUtil
{
    /**
     * Get the lines of text from a reader.
     * 
     * @param reader to read from, will be closed.
     * @return the lines contained within.
     */
    static Iterable<String> getLines(final Reader reader)
    {
        return transform(reader, GetRawLines.INSTANCE);
    }

    static <T> T transform(final Reader reader, final Transformer<T> transformer)
    {
        try
        {
            return transformer.get(reader);
        }
        catch (final IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            IOUtils.closeQuietly(reader);
        }
    }

    interface Transformer<T>
    {
        T get(Reader reader) throws IOException;
    }

    /**
     * Gets all lines from a reader.
     */
    enum GetRawLines implements ReaderUtil.Transformer<Iterable<String>>
    {
        INSTANCE;

        public Iterable<String> get(final Reader reader) throws IOException
        {
            @SuppressWarnings("unchecked")
            final Iterable<String> lines = readLines(reader);
            return lines;
        }
    }
}