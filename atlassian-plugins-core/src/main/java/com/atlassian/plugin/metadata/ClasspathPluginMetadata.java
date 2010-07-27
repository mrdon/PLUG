package com.atlassian.plugin.metadata;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.util.ClassLoaderUtils;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import static com.google.common.collect.Iterables.filter;
import static org.apache.commons.io.IOUtils.readLines;

class ClasspathPluginMetadata implements PluginMetadata
{
    static final Logger log = LoggerFactory.getLogger(ClasspathPluginMetadata.class);
    static final String APPLICATION_PROVIDED_PLUGINS_FILENAME = "application-provided-plugins.txt";
    static final String REQUIRED_PLUGINS_FILENAME = "application-required-plugins.txt";
    static final String REQUIRED_MODULES_FILENAME = "application-required-modules.txt";

    private final Set<String> pluginKeys;
    private final Set<String> requiredPluginKeys;
    private final Set<String> requiredModules;

    public ClasspathPluginMetadata()
    {
        this(InputStreamFromClasspath.INSTANCE);
    }

    public ClasspathPluginMetadata(final Function<String, Collection<InputStream>> loader)
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

    static Set<String> getStringsFromFile(final String fileName, final Function<String, Collection<InputStream>> loader)
    {
        final ImmutableList.Builder<String> stringsFromFiles = ImmutableList.builder();
        final Collection<InputStream> fileInputStreams = loader.apply(fileName);

        for (InputStream fileInputStream : fileInputStreams)
        {
            try
            {
                if (fileInputStream != null)
                {
                    try
                    {
                        @SuppressWarnings("unchecked")
                        final List<String> lines = readLines(fileInputStream);
                        // Make sure that we trim the strings that we read from the file and filter out comments and blank lines
                        stringsFromFiles.addAll(filter(Iterables.transform(lines, TrimString.INSTANCE), NotComment.INSTANCE));
                    }
                    catch (final IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
            finally
            {
                IOUtils.closeQuietly(fileInputStream);
            }
        }
        return ImmutableSet.copyOf(stringsFromFiles.build());
    }

    ///CLOVER:OFF
    enum InputStreamFromClasspath implements Function<String, Collection<InputStream>>
    {
        INSTANCE;

        public Collection<InputStream> apply(final String fileName)
        {
            final Collection<InputStream> inputStreams = new ArrayList<InputStream>();
            try
            {
                final Class<ClasspathPluginMetadata> clazz = ClasspathPluginMetadata.class;
                final Enumeration<URL> urlEnumeration = clazz.getClassLoader().getResources(clazz.getPackage().getName().replace(".", "/") + "/" + fileName);
                while (urlEnumeration.hasMoreElements())
                {
                    inputStreams.add(urlEnumeration.nextElement().openStream());
                }
                return inputStreams;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
    ///CLOVER:ON

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
            // Don't include blank lines or lines that start with a comment syntax
            return StringUtils.isNotBlank(input) && !input.startsWith("#");
        }
    }
}