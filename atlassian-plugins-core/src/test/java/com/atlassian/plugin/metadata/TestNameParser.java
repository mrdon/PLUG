package com.atlassian.plugin.metadata;

import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.size;

import java.io.StringReader;

import junit.framework.TestCase;

import com.google.common.collect.Iterables;

public class TestNameParser extends TestCase
{
    public void testSimple()
    {
        assertLineParse("test1", "test1");
    }

    public void testTrimStart()
    {
        assertLineParse("test2", "  test2");
    }

    public void testTrimEnd()
    {
        assertLineParse("test3", "test3   ");
    }

    public void testBlankStart()
    {
        assertLineParse("test4", "\n  \n  test4");
    }

    public void testBlankEnd()
    {
        assertLineParse("test5", "test5   \n   \n");
    }

    public void testBlankStartEnd()
    {
        assertLineParse("test6", "  \n \n   \ttest6\t   \n   \n");
    }

    public void testIgnoreEmpty()
    {
        assertEmptyParse("");
    }

    public void testIgnoreBlank()
    {
        assertEmptyParse("   \t \t");
    }

    public void testIgnoreAllBlankLines()
    {
        assertEmptyParse("  \n\t\n \t\n\t\t\t   \n");
    }

    public void testIgnoreComment()
    {
        assertEmptyParse("# this is a comment");
    }

    public void testIgnoreMultipleComments()
    {
        assertEmptyParse("#\n# this is a comment\n#And so is this\n#");
    }

    public void testIgnoreMultipleCommentsAndBlankLines()
    {
        assertEmptyParse("  \n\t\n \t\n\t\t\t   \n#\n# this is a comment\n#And so is this\n#");
    }

    public void testMultiline()
    {
        final Iterable<String> lines = new NameParser().apply(new StringReader("one\ntwo\nthree"));
        assertEquals(3, size(lines));
        assertEquals("one", get(lines, 0));
        assertEquals("two", get(lines, 1));
        assertEquals("three", get(lines, 2));
    }

    public void testMultilineWithCommentsAndBlanks()
    {
        final Iterable<String> lines = new NameParser().apply(new StringReader("  one  \n#comment\n\n  two\n### deary\n\r\r#\rthree   "));
        assertEquals(3, size(lines));
        assertEquals("one", get(lines, 0));
        assertEquals("two", get(lines, 1));
        assertEquals("three", get(lines, 2));
    }

    static void assertLineParse(final String expect, final String input)
    {
        assertEquals(expect, getOnlyElement(new NameParser().apply(new StringReader(input))));
    }

    static void assertEmptyParse(final String input)
    {
        assertTrue(input, Iterables.isEmpty(new NameParser().apply(new StringReader(input))));
    }
}
