package com.atlassian.plugin.util;

import junit.framework.TestCase;
import static com.atlassian.plugin.util.EfficientStringUtils.endsWith;

public class EfficientStringUtilsTest extends TestCase
{
    public void testEndsWith()
    {
        assertTrue(endsWith("abc", "c"));
        assertTrue(endsWith("foo.xml", ".", "xml"));
        assertTrue(endsWith("foo", "foo"));
    }

    public void testEndsWithNoMatchingSuffix()
    {
        assertFalse(endsWith("foo","ooo"));
        assertFalse(endsWith("foo.xml", "."));
    }

    public void testEndsWithEmptySuffixes()
    {
        // Degenerate cases: any string ends with nothing
        assertTrue(endsWith("foo",""));
        assertTrue(endsWith("",""));
        assertTrue(endsWith("foo"));
        assertTrue(endsWith(""));
    }
}
