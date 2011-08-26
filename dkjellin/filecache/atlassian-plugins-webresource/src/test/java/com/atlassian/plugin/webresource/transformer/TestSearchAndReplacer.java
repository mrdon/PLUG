package com.atlassian.plugin.webresource.transformer;

import com.google.common.base.Function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class TestSearchAndReplacer extends TestCase
{
    public void testSimple()
    {
        final String input = "1 two 3 four 5";

        final Function<Matcher, CharSequence> function = new Function<Matcher, CharSequence>()
        {
            public CharSequence apply(final Matcher m)
            {
                return new StringBuilder("$").append(m.group().toUpperCase()).append("\\");
            }
        };
        final SearchAndReplacer grep = new SearchAndReplacer(Pattern.compile("[a-zA-Z]+"), function);
        final String output = grep.replaceAll(input).toString();

        assertEquals("1 $TWO\\ 3 $FOUR\\ 5", output);
    }
}
