package com.atlassian.plugin.metadata;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.filter;
import static org.apache.commons.io.IOUtils.readLines;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.descriptors.CannotDisable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

/**
 * A default implementation that looks at the com.atlassian.plugin.metadata package of the classpath for files named:
 * <ul>
 *   <li>application-provided-plugins*.txt - used to list the plugin keys of all plugins that are provided by the host application</li>
 *   <li>application-required-plugins*.txt - used to list the plugin keys that are considered required for the application to function correctly</li>
 *   <li>application-required-modules*.txt - used to list the module keys that are considered required for the application to function correctly</li>
 * </ul>
 *
 * Note the '*' in the file names above. This class will scan the package for all files named with the prefix (before the *) and
 * the suffix (after the *). For example, this will find a file named application-provided-plugins.txt as well as a file
 * named application-provided-plugins-my.crazy-file.1.0.txt. Both files contents will be used to inform this implementation
 * of plugin keys.
 *
 * @since 2.6.0 
 */
public class DefaultPluginMetadataManager implements PluginMetadataManager
{
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginMetadataManager.class);
    static final String APPLICATION_PROVIDED_PLUGINS_FILENAME_PREFIX = "application-provided-plugins";
    static final String REQUIRED_PLUGINS_FILENAME_PREFIX = "application-required-plugins";
    static final String REQUIRED_MODULES_FILENAME_PREFIX = "application-required-modules";
    static final String DOT = ".";
    static final String FILENAME_EXTENSION = "txt";

    private final Collection<String> applicationProvidedPlugins;
    private final Collection<String> requiredPlugins;
    private final Collection<String> requiredModules;
    private final Function<String, String> transformInputFunction;
    private final Predicate<String> filterInputPredicate;

    public DefaultPluginMetadataManager()
    {
        transformInputFunction = new TransformInputFunction();
        filterInputPredicate = new FilterInputPredicate();
        applicationProvidedPlugins = getStringsFromFiles(APPLICATION_PROVIDED_PLUGINS_FILENAME_PREFIX, FILENAME_EXTENSION);
        requiredPlugins = getStringsFromFiles(REQUIRED_PLUGINS_FILENAME_PREFIX, FILENAME_EXTENSION);
        requiredModules = getStringsFromFiles(REQUIRED_MODULES_FILENAME_PREFIX, FILENAME_EXTENSION);
    }

    /**
     * A plugin is determined to be non-user if {@link com.atlassian.plugin.Plugin#isBundledPlugin()} is true
     * or if the host application has indicated to the plugins system that a plugin was provided by it.
     * NOTE: If a user has upgraded a bundled plugin then the decision of whether it is user installed plugin
     * is determined by if the application has indicated to the plugins system that a plugin was provided or not.
     */
    public boolean isUserInstalled(final Plugin plugin)
    {
        checkNotNull(plugin, "plugin");
        // It is user installed if it has not been marked as provided by the application and it was not bundled.
        return !plugin.isBundledPlugin() && !applicationProvidedPlugins.contains(plugin.getKey());
    }

    /**
     * A plugin is determined to be optional if the host application has not indicated to the plugins system that
     * it is required or if any of its modules have been flagged as not optional.
     */
    public boolean isOptional(final Plugin plugin)
    {
        checkNotNull(plugin, "plugin");
        // If the application has marked the plugin as required then we know we are required
        if (!optionalAccordingToHostApplication(plugin))
        {
            return false;
        }

        // We need to check if any of the plugins modules are not optional
        for (final ModuleDescriptor<?> moduleDescriptor : plugin.getModuleDescriptors())
        {
            if (!optionalAccordingToHostApplication(moduleDescriptor))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * A module is determined to be optional if the host application
     * has not indicated to the plugins system that it is required. If the call to {@code isOptional} with the module
     * descriptor's plugin is {@code false}, then this method will also return {@code false}.  Also if the module descriptor is
     * annotated with {@link com.atlassian.plugin.descriptors.CannotDisable} then it can not be optional.
     */
    public boolean isOptional(final ModuleDescriptor<?> moduleDescriptor)
    {
        checkNotNull(moduleDescriptor, "moduleDescriptor");
        // It is not optional if the host application has marked it as required
        if (!optionalAccordingToHostApplication(moduleDescriptor))
        {
            return false;
        }

        // A module can not be optional if it is marked by the CannotDisable annotation
        if (!optionalAccordingToModuleDescriptorType(moduleDescriptor))
        {
            return false;
        }

        // A module can only be optional if its parent plugin is not declared by the host application as required
        return optionalAccordingToHostApplication(moduleDescriptor.getPlugin());
    }

    private boolean optionalAccordingToHostApplication(final Plugin plugin)
    {
        return !requiredPlugins.contains(plugin.getKey());
    }

    private boolean optionalAccordingToHostApplication(final ModuleDescriptor<?> moduleDescriptor)
    {
        return !requiredModules.contains(moduleDescriptor.getCompleteKey());
    }

    private boolean optionalAccordingToModuleDescriptorType(final ModuleDescriptor<?> moduleDescriptor)
    {
        return !moduleDescriptor.getClass().isAnnotationPresent(CannotDisable.class);
    }

    private Collection<String> getStringsFromFiles(final String fileNamePrefix, final String fileNameExtension)
    {
        final Collection<String> stringsFromFiles = new ArrayList<String>();
        final String[] fileNames = getMatchingFileNamesFromClasspath(fileNamePrefix, fileNameExtension);

        for (final String fileName : fileNames)
        {
            final InputStream providedPluginsStream = getInputStreamFromClasspath(fileName);
            try
            {
                if (providedPluginsStream != null)
                {
                    try
                    {
                        // Make sure that we trim the strings that we read from the file, we copy the list so that the transformation only occurs once
                        stringsFromFiles.addAll(copyOf(filter(Lists.<String, String> transform(readLines(providedPluginsStream),
                            transformInputFunction), filterInputPredicate)));
                    }
                    catch (final IOException e)
                    {
                        log.error("Unable to read from the input stream provided by '" + fileName + "'.", e);
                    }
                }
            }
            finally
            {
                IOUtils.closeQuietly(providedPluginsStream);
            }

        }
        return stringsFromFiles;
    }

    private class TransformInputFunction implements Function<String, String>
    {
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

    private class FilterInputPredicate implements Predicate<String>
    {
        public boolean apply(@Nullable final String input)
        {
            // Don't include blank lines or lines that start with a comment syntax
            return StringUtils.isNotBlank(input) && !input.startsWith("#");
        }
    }

    ///CLOVER:OFF
    InputStream getInputStreamFromClasspath(final String fileName)
    {
        return getClass().getResourceAsStream(fileName);
    }

    ///CLOVER:ON

    /**
     * Looks at the package of this class to see what files exist in the package that match the provided prefix and
     * file extension.
     *
     * @param fileNamePrefix the beginning of the filename
     * @param fileNameExtension the extension of the filename
     * @return a list of file names that matches the prefix + wildcard + extension or fileName + DOT + extension if none
     * were found.
     */
    String[] getMatchingFileNamesFromClasspath(final String fileNamePrefix, final String fileNameExtension)
    {
        String[] matchingFiles = null;
        // Get a File object for the package
        final URL resource = getClass().getResource("/" + getClass().getPackage().getName().replace(".", "/"));
        if (resource != null)
        {
            final File directory = new File(resource.getFile());
            if (directory.exists())
            {
                matchingFiles = directory.list(new FilenameFilter()
                {
                    public boolean accept(final File dir, final String name)
                    {
                        return name.matches(fileNamePrefix + ".*" + fileNameExtension);
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
}
