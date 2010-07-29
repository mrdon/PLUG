package com.atlassian.plugin.metadata;

import static com.google.common.collect.Iterators.forEnumeration;
import static com.google.common.collect.Iterators.transform;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;

import net.jcip.annotations.ThreadSafe;

import com.atlassian.plugin.metadata.PluginMetadata.Type;
import com.google.common.base.Function;

/**
 * {@link ReaderFactory} that searches the class path for files with the correct
 * {@link Type#getFileName() name} and opens readers for them all.
 * 
 * @since 2.6
 */
@ThreadSafe
final class ClasspathReaderFactory implements ReaderFactory
{
    static final Charset UTF8 = Charset.forName("UTF-8");

    public Iterable<Reader> getReaders(final Type type)
    {
        return new Iterable<Reader>()
        {
            public Iterator<Reader> iterator()
            {
                return transform(urls(named(type.getFileName())), UrlToReader.INSTANCE);
            }
        };
    }

    /**
     * Get the proper qualified resource name for the supplied name.
     */
    String named(final String name)
    {
        return getClass().getPackage().getName().replace(".", "/") + "/" + name;
    }

    /**
     * Get the {@link URL urls} for resources found on the classpath with the
     * supplied qualified name.
     */
    Iterator<URL> urls(final String resourceName)
    {
        try
        {
            return forEnumeration(getClass().getClassLoader().getResources(resourceName));
        }
        catch (final IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens an {@link Reader} from a {@link URL}. Assumes a UTF-8 charset.
     */
    enum UrlToReader implements Function<URL, Reader>
    {
        INSTANCE;

        public Reader apply(final URL from)
        {
            try
            {
                return new InputStreamReader(from.openStream(), UTF8);
            }
            catch (final IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
