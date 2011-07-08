package com.atlassian.plugin.webresource.transformer;

import com.google.common.base.Function;
import junit.framework.TestCase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestSearchAndReplacer extends TestCase {
    public void testSimple() {

        String input = "1 two 3 four 5";

        Function<Matcher, String> function = new Function<Matcher, String>() {
            public String apply(Matcher m) {
                return "$" + m.group().toUpperCase() + "\\";
            }
        };
        SearchAndReplacer grep = new SearchAndReplacer(Pattern.compile("[a-zA-Z]+"), function);
        String output = grep.replaceAll(input);

        assertEquals("1 $TWO\\ 3 $FOUR\\ 5", output);

    }
}
