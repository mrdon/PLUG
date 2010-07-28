package com.atlassian.plugin.metadata;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterators.forEnumeration;
import static com.google.common.collect.Iterators.transform;
import static org.apache.commons.io.IOUtils.readLines;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

final class ClasspathPluginMetadata implements PluginMetadata
{
    static final Logger log = LoggerFactory.getLogger(ClasspathPluginMetadata.class);
    static final String APPLICATION_PROVIDED_PLUGINS_FILENAME = "application-provided-plugins.txt";
    static final String REQUIRED_PLUGINS_FILENAME = "application-required-plugins.txt";
    static final String REQUIRED_MODULES_FILENAME = "application-required-modules.txt";

    private final Set<String> pluginKeys;
    private final Set<String> requiredPluginKeys;
    private final Set<String> requiredModules;

    ClasspathPluginMetadata()
    {
        this(InputStreamFromClasspath.INSTANCE);
    }

    ClasspathPluginMetadata(final Function<String, Iterable<InputStream>> loader)
    {
        pluginKeys = getStringsFromFile(APPLICATION_PROVIDED_PLUGINS_FILENAME, loader);
        requiredPluginKeys = getStringsFromFile(REQUIRED_PLUGINS_FILENAME, loader);
        requiredModules = getStringsFromFile(REQUIRED_MODULES_FILENAME, loader);
    }

    public boolean applicationProvided(final Plugin plugin)
    {
        return pluginKeys.contains(plugin.getKey());
    }

    public boolean required(final Plugin plugin)
    {
        return requiredPluginKeys.contains(plugin.getKey());
    }

    public boolean required(final ModuleDescriptor<?> descriptor)
    {
        return requiredModules.contains(descriptor.getCompleteKey());
    }

    static Set<String> getStringsFromFile(final String fileName, final Function<String, Iterable<InputStream>> loader)
    {
        final ImmutableList.Builder<String> stringsFromFiles = ImmutableList.builder();
        for (final InputStream stream : loader.apply(fileName))
        {
            try
            {
                if (stream != null)
                {
                    try
                    {
                        @SuppressWarnings("unchecked")
                        final Iterable<String> lines = readLines(stream);
                        // Make sure that we trim the strings that we read from
                        // the file and filter out comments and blank lines
                        stringsFromFiles.addAll(filter(transform(lines, TrimString.INSTANCE), NotComment.INSTANCE));
                    }
                    catch (final IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
            finally
            {
                IOUtils.closeQuietly(stream);
            }
        }
        return ImmutableSet.copyOf(stringsFromFiles.build());
    }

    // /CLOVER:OFF
    /**
     * Get all resources with the supplied name as an {@link Iterable} of opened
     * {@link InputStream streams}.
     */
    enum InputStreamFromClasspath implements Function<String, Iterable<InputStream>>
    {
        INSTANCE;

        public Iterable<InputStream> apply(final String fileName)
        {
            return new Iterable<InputStream>()
            {

                public Iterator<InputStream> iterator()
                {
                    final Class<ClasspathPluginMetadata> clazz = ClasspathPluginMetadata.class;
                    final String resourceName = clazz.getPackage().getName().replace(".", "/") + "/" + fileName;
                    try
                    {
                        return transform(forEnumeration(clazz.getClassLoader().getResources(resourceName)), UrlToInputStream.INSTANCE);
                    }
                    catch (final IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }

    // /CLOVER:ON

    /**
     * Opens an {@link InputStream} from a {@link URL}.
     */
    enum UrlToInputStream implements Function<URL, InputStream>
    {
        INSTANCE;
        public InputStream apply(final URL from)
        {
            try
            {
                return from.openStream();
            }
            catch (final IOException e)
            {
                throw new RuntimeException(e);
            }
        }
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
    enum NotComment implements Predicate<String>
    {
        INSTANCE;

        public boolean apply(@Nullable final String input)
        {
            // Don't include blank lines or lines that start with a comment
            // syntax
            return StringUtils.isNotBlank(input) && !input.startsWith("#");
        }
    }
}