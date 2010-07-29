package com.atlassian.plugin.metadata;

import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Iterables.size;

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

import junit.framework.TestCase;

import com.atlassian.plugin.metadata.PluginMetadata.Type;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

public class TestDefaultMetadataLoader extends TestCase
{
    public void testConcatenate()
    {
        final Iterable<String> readers = new DefaultMetadataLoader(factory("one", "two")).load(Type.ApplicationProvided);

        assertEquals(2, size(readers));
        assertEquals("one", get(readers, 0));
        assertEquals("two", get(readers, 1));
    }

    public void testMultiple()
    {
        final Iterable<String> readers = new DefaultMetadataLoader(factory("one\ntwo")).load(Type.ApplicationProvided);

        assertEquals(2, size(readers));
        assertEquals("one", get(readers, 0));
        assertEquals("two", get(readers, 1));
    }

    public void testMultipleWithBlanks()
    {
        final Iterable<String> readers = new DefaultMetadataLoader(factory("\n\none\n\n\ntwo\n\n")).load(Type.ApplicationProvided);

        assertEquals(2, size(readers));
        assertEquals("one", get(readers, 0));
        assertEquals("two", get(readers, 1));
    }

    public void testMultipleWithComments()
    {
        final Iterable<String> readers = new DefaultMetadataLoader(factory("\n# number1\none\n\n### no2\ntwo\n\n")).load(Type.ApplicationProvided);

        assertEquals(2, size(readers));
        assertEquals("one", get(readers, 0));
        assertEquals("two", get(readers, 1));
    }

    public void testConcatenateMultipleWithCommentsAndBlanks()
    {
        final ReaderFactory factory = factory("\n# number1\none\n\n### no2\ntwo\n\n", "\n\n#\n# number3\n#\nthree\n\n### no4\nfour\n\n");
        final Iterable<String> readers = new DefaultMetadataLoader(factory).load(Type.ApplicationProvided);

        assertEquals(4, size(readers));
        assertEquals("one", get(readers, 0));
        assertEquals("two", get(readers, 1));
        assertEquals("three", get(readers, 2));
        assertEquals("four", get(readers, 3));
    }

    static ReaderFactory factory(final String... strings)
    {
        return new ReaderFactory()
        {
            public Iterable<Reader> getReaders(final Type type)
            {
                return readers(strings);
            }
        };
    }

    static Iterable<Reader> readers(final String... strings)
    {
        return Iterables.transform(Arrays.asList(strings), new Function<String, Reader>()
        {
            public Reader apply(final String from)
            {
                return new StringReader(from);
            }
        });
    }
}
