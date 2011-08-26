package com.atlassian.plugin.webresource.transformer;

import com.google.common.base.Function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs a search and replace function on a given input.
 * @since 2.9.0
 */
public class SearchAndReplacer
{
    public static SearchAndReplacer create(final Pattern pattern, final Function<Matcher, CharSequence> replacer)
    {
        return new SearchAndReplacer(pattern, replacer);
    }

    private final Pattern pattern;
    private final Function<Matcher, CharSequence> replacer;

    /**
     * @param pattern the pattern to find in the input
     * @param replacer a function that gives replacement text for the given match
     */
    SearchAndReplacer(final Pattern pattern, final Function<Matcher, CharSequence> replacer)
    {
        this.pattern = pattern;
        this.replacer = replacer;
    }

    /**
     * Replace all occurrences of the pattern in the input, using the given function
     */
    public CharSequence replaceAll(final CharSequence input)
    {
        final Matcher matcher = pattern.matcher(input);
        final StringBuffer output = new StringBuffer();
        while (matcher.find())
        {
            final CharSequence sequence = replacer.apply(matcher);
            matcher.appendReplacement(output, "");
            output.append(sequence);
        }
        matcher.appendTail(output);
        return output;
    }
}
