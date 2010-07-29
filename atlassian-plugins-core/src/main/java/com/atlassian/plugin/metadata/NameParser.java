package com.atlassian.plugin.metadata;

import static com.atlassian.plugin.metadata.ReaderUtil.getLines;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

import java.io.Reader;

import javax.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Parses the names. Takes a {@link Reader} as input.
 * <p>
 * Reads the individual lines and filters blank lines and comments.
 * 
 * @since 2.6
 */
@ThreadSafe
final class NameParser implements Function<Reader, Iterable<String>>
{
    public Iterable<String> apply(final Reader reader)
    {
        // get the lines and trim them
        // then filter out comments and blank lines
        return filter(transform(getLines(reader), TrimString.INSTANCE), NotBlankOrComment.INSTANCE);
    }

    /**
     * Return a trimmed string.
     */
    enum TrimString implements Function<String, String>
    {
        INSTANCE;

        public String apply(final String from)
        {
            // This should never be true
            if (from == null)
            {
                return from;
            }
            return from.trim();
        }
    }

    /**
     * Remove blank strings and hash delimited comments.
     */
    enum NotBlankOrComment implements Predicate<String>
    {
        INSTANCE;

        public boolean apply(@Nullable final String input)
        {
            // Don't include blank lines or lines that start with a comment
            // char
            return StringUtils.isNotBlank(input) && !input.startsWith("#");
        }
    }
}