package com.atlassian.plugin.metadata;

import static com.google.common.collect.Iterables.filter;
import static org.apache.commons.io.IOUtils.readLines;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

class ClasspathPluginMetadata implements PluginMetadata
{
    static final Logger log = LoggerFactory.getLogger(ClasspathPluginMetadata.class);
    static final String APPLICATION_PROVIDED_PLUGINS_FILENAME_PREFIX = "application-provided-plugins";
    static final String REQUIRED_PLUGINS_FILENAME_PREFIX = "application-required-plugins";
    static final String REQUIRED_MODULES_FILENAME_PREFIX = "application-required-modules";
    static final String DOT = ".";
    static final String FILENAME_EXTENSION = "txt";

    private final Set<String> pluginKeys;
    private final Set<String> requiredPluginKeys;
    private final Set<String> requiredModules;

    public ClasspathPluginMetadata()
    {
        this(InputStreamFromClasspath.INSTANCE);
    }

    public ClasspathPluginMetadata(final Function<String, InputStream> loader)
    {
        pluginKeys = getStringsFromFiles(APPLICATION_PROVIDED_PLUGINS_FILENAME_PREFIX, FILENAME_EXTENSION, loader);
        requiredPluginKeys = getStringsFromFiles(REQUIRED_PLUGINS_FILENAME_PREFIX, FILENAME_EXTENSION, loader);
        requiredModules = getStringsFromFiles(REQUIRED_MODULES_FILENAME_PREFIX, FILENAME_EXTENSION, loader);
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

    static Set<String> getStringsFromFiles(final String fileNamePrefix, final String fileNameExtension, final Function<String, InputStream> loader)
    {
        final ImmutableList.Builder<String> stringsFromFiles = ImmutableList.builder();
        final String[] fileNames = getMatchingFileNamesFromClasspath(fileNamePrefix, fileNameExtension);

        for (final String fileName : fileNames)
        {
            final InputStream providedPluginsStream = loader.apply(fileName);
            try
            {
                if (providedPluginsStream != null)
                {
                    try
                    {
                        @SuppressWarnings("unchecked")
                        final List<String> lines = readLines(providedPluginsStream);
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
                IOUtils.closeQuietly(providedPluginsStream);
            }
        }
        return ImmutableSet.copyOf(stringsFromFiles.build());
    }

    /**
     * Looks at the package of this class to see what files exist in the package that match the provided prefix and
     * file extension.
     *
     * @param fileNamePrefix the beginning of the filename
     * @param fileNameExtension the extension of the filename
     * @return a list of file names that matches the prefix + wildcard + extension or fileName + DOT + extension if none
     * were found.
     */
    static String[] getMatchingFileNamesFromClasspath(final String fileNamePrefix, final String fileNameExtension)
    {
        String[] matchingFiles = null;
        // Get a File object for the package
        final Class<ClasspathPluginMetadata> klass = ClasspathPluginMetadata.class;
        final URL resource = klass.getResource("/" + klass.getPackage().getName().replace(".", "/"));
        if (resource != null)
        {
            final File directory = new File(resource.getFile());
            if (directory.exists())
            {
                matchingFiles = directory.list(new FilenameFilter()
                {
                    private final Pattern pattern = Pattern.compile(fileNamePrefix + ".*" + fileNameExtension);

                    public boolean accept(final File dir, final String name)
                    {
                        return pattern.matcher(name).matches();
                    }
                });
            }
        }
        if ((matchingFiles == null) || (matchingFiles.length == 0))
        {
            // If we can not read the jar directory or we did not find any then lets just fallback to the filename + DOT + extension
            return new String[] { fileNamePrefix + DOT + fileNameExtension };
        }
        return matchingFiles;
    }

    ///CLOVER:OFF
    enum InputStreamFromClasspath implements Function<String, InputStream>
    {
        INSTANCE;

        public InputStream apply(final String fileName)
        {
            return ClasspathPluginMetadata.class.getResourceAsStream(fileName);
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