package com.atlassian.plugin.metadata;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import net.jcip.annotations.ThreadSafe;

import com.atlassian.plugin.metadata.PluginMetadata.Type;

/**
 * Default implementation of {@link MetadataLoader} that uses a
 * {@link ReaderFactory} to access the raw contents of files or other text data
 * and parse the names from it.
 * 
 * @since 2.6
 */
@ThreadSafe
final class DefaultMetadataLoader implements MetadataLoader
{
    /**
     * Load the {@link PluginMetadata} from the classpath.
     */
    static MetadataLoader loadFromClasspath()
    {
        return new DefaultMetadataLoader(new ClasspathReaderFactory());
    }

    private final ReaderFactory readers;
    // parse the contents of the supplied readers
    private final NameParser parser = new NameParser();

    DefaultMetadataLoader(final ReaderFactory readers)
    {
        this.readers = checkNotNull(readers, "readers");
    }

    public Iterable<String> load(final Type type)
    {
        return concat(transform(readers.getReaders(type), parser));
    }
}