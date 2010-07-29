package com.atlassian.plugin.metadata;

import static org.apache.commons.io.IOUtils.readLines;

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.io.IOUtils;

/**
 * Utility for managing (reading data from and transforming) and closing a
 * {@link Reader}.
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
     * @throws RuntimeException if an IOException is encountered while reading
     */
    static Iterable<String> getLines(final Reader reader)
    {
        return transform(reader, GetRawLines.INSTANCE);
    }

    /**
     * Transform the contents of a {@link Reader} into some type (T) using a
     * {@link Transformer}. Closes the reader afterwards and rethrows any
     * {@link IOException} as an unchecked exception.
     * 
     * @param <T> the return type the transformer will create
     * @param reader the raw character data
     * @param transformer for transforming the raw data into something
     * @return the transformer's result
     * @throws RuntimeException if an IOException is encountered while reading
     */
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

    /**
     * Given a {@link Reader}, turn it into something.
     * 
     * @param <T> the result type
     */
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