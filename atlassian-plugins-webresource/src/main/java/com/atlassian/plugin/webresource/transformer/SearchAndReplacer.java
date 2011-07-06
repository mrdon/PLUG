package com.atlassian.plugin.webresource.transformer;

import com.google.common.base.Function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs a search and replace function on a given input.
 * @since 2.10
 */
public class SearchAndReplacer
{
    private final Pattern pattern;
    private final Function<Matcher, String> replacer;

    /**
     * @param pattern the pattern to find in the input
     * @param replacer a function that gives replacement text for the given match
     */
    public SearchAndReplacer(final Pattern pattern, final Function<Matcher, String> replacer)
    {
        this.pattern = pattern;
        this.replacer = replacer;
    }

    public SearchAndReplacer(final String pattern, final Function<Matcher, String> replacer)
    {
        this(Pattern.compile(pattern), replacer);
    }

    /**
     * Replace all occurrences of the pattern in the input, using the given function
     */
    public String replaceAll(final String input)
    {
        final Matcher matcher = pattern.matcher(input);
        final StringBuffer output = new StringBuffer();
        while (matcher.find())
        {
            final String r = replacer.apply(matcher);
            matcher.appendReplacement(output, "");
            output.append(r);
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
