package com.atlassian.plugin.metadata;

import java.io.StringReader;

import junit.framework.TestCase;

import com.google.common.collect.Iterables;

public class TestReaderUtil extends TestCase
{
    public void testSingle()
    {
        assertLines("test", "test");
    }

    public void testDouble()
    {
        assertLines("one\ntwo", "one", "two");
    }

    public void testIgnoreFinalNewline()
    {
        assertLines("one\ntwo\n", "one", "two");
    }

    public void testWithBlank()
    {
        assertLines("one\n\ntwo", "one", "", "two");
    }

    public void testBlankStart()
    {
        assertLines("\none\ntwo", "", "one", "two");
    }

    public void testBlankEnd()
    {
        assertLines("one\ntwo\n\n", "one", "two", "");
    }

    static void assertLines(final String input, final String... expected)
    {
        final Iterable<String> lines = ReaderUtil.getLines(new StringReader(input));
        assertEquals(expected.length, Iterables.size(lines));
        for (int i = 0; i < expected.length; i++)
        {
            assertEquals(expected[i], Iterables.get(lines, i));
        }
    }
}
