package com.atlassian.plugin.util;

import junit.framework.TestCase;

public class TestVersionStringComparator extends TestCase
{
    private VersionStringComparator comparator = new VersionStringComparator();

    public void testNonStringsEqual()
    {
        assertBothSame(new Long(25), new byte[]{ 0 });
    }

    public void testIntegers()
    {
        assertSecondGreater("0", "1");
        assertSecondGreater("1", "2");
        assertSecondGreater("75", "1000");
    }

    public void testOneDot()
    {
        assertSecondGreater("0.1", "0.2");
        assertSecondGreater("0.9", "1.0");
        assertSecondGreater("1.1", "1.10");
        assertSecondGreater("1.1", "1.19");
        assertSecondGreater("3.75", "4.19");
        assertSecondGreater("0.1", "1");
        assertSecondGreater("0.9", "1");
    }

    public void testTwoDots()
    {
        assertSecondGreater("0.0.1", "0.0.2");
        assertSecondGreater("0.9.0", "1.0.0");
        assertSecondGreater("1.0", "1.0.1");
        assertSecondGreater("1.1.0", "1.10.0");
        assertSecondGreater("1.1.9", "1.19");
        assertSecondGreater("3.56.75", "4.46.19");
    }

    public void testMoreDots()
    {
        assertSecondGreater("0.0.0.1", "0.0.0.2");
        assertSecondGreater("0.0.0.1", "0.0.1.0");
        assertSecondGreater("2.5.7.3", "2.5.8.1");
        assertSecondGreater("27.5.27.3.12", "56.5.8.1");
    }

    public void testLetters()
    {
        assertSecondGreater("0.1a", "0.1b");
        assertSecondGreater("0.1-alpha", "0.2-beta");
        assertSecondGreater("2.3-dr1", "2.3-dr2");
        assertSecondGreater("2.3-dr1", "2.3-DR2");
        assertSecondGreater("2.3-dr1", "2.3");
        assertSecondGreater("1.0-rc1", "1.0");
        assertSecondGreater("1.0a", "1.1");
        assertSecondGreater("1.0a", "1.1a");
        assertSecondGreater("1.0a", "2.0a");
        assertSecondGreater("1.5", "1.6a");
    }

    public void testBetas()
    {
        assertSecondGreater("1.0-beta1", "1.0-beta2");
        assertSecondGreater("1.0-beta", "1.0-beta2");
    }

    public void testDates()
    {
        assertSecondGreater("2006-01-12", "2006-10-07");
        assertSecondGreater("2006.01.12", "2006-10-07");
        assertSecondGreater("20060112", "20061007");
    }

    /**
     * Unfortunate side-effects of the implementation that can't be avoided
     */
    public void testUnintuitiveCases()
    {
        assertBothSame("1.01", "1.1");
        assertSecondGreater("1.0a", "1.0");
        assertSecondGreater("1.0-beta10", "1.0-beta2");
    }

    public void testValidVersions()
    {
        assertValidVersion("1");
        assertValidVersion("1.0");
        assertValidVersion("1.2.0");
        assertValidVersion("1.2.0.56");
        assertValidVersion("0.223.0.56");
        assertValidVersion("2.3-beta1");
        assertValidVersion("alpha");
        assertValidVersion("beta");
        assertValidVersion("2,1,3");
        assertValidVersion("2005-12-03");
    }

    public void testInvalidVersions()
    {
        assertInvalidVersion("");
        assertInvalidVersion(null);
        assertInvalidVersion("%^&%#");
    }

    private void assertSecondGreater(Object first, Object second)
    {
        assertValidVersion(first.toString());
        assertValidVersion(second.toString());

        // check both are reflexive
        assertBothSame(first, first);
        assertBothSame(second, second);

        // check for symmetry
        assertEquals(first.toString() + " < " + second.toString(), 1, comparator.compare(second, first));
        assertEquals(first.toString() + " < " + second.toString(), -1, comparator.compare(first, second));
    }

    private void assertValidVersion(String string)
    {
        assertTrue(VersionStringComparator.isValidVersionString(string));
    }

    private void assertInvalidVersion(String string)
    {
        assertFalse(VersionStringComparator.isValidVersionString(string));
    }

    private void assertBothSame(Object first, Object second)
    {
        assertEquals(first.toString() + " == " + second.toString(), 0, comparator.compare(first, second));
    }
}
