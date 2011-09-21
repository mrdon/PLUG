package com.atlassian.plugin.metadata;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import static org.apache.commons.io.IOUtils.readLines;

/**
 * Looks on the classpath for three files named: <ul> <li>application-provided-plugins.txt - used to list the plugin
 * keys of all plugins that are provided by the host application</li> <li>application-required-plugins.txt - used to
 * list the plugin keys that are considered required for the application to function correctly</li>
 * <li>application-required-modules.txt - used to list the module keys that are considered required for the application
 * to function correctly</li> </ul> Note that all files in that package space with those names will be included.
 * <p/>
 * All files contents will be used to inform this implementation of plugin keys. This will read the contents all
 * instances of those files into the structures of this class.
 * <p/>
 * The values will determine the plugin metadata for this implementation.
 *
 * @since 2.6
 */
public class ClasspathFilePluginMetadata implements PluginMetadata, RequiredPluginProvider
{
    final static String APPLICATION_PROVIDED_PLUGINS_FILENAME = "application-provided-plugins.txt";
    final static String APPLICATION_REQUIRED_PLUGINS_FILENAME = "application-required-plugins.txt";
    final static String APPLICATION_REQUIRED_MODULES_FILENAME = "application-required-modules.txt";

    private final Set<String> providedPluginKeys;
    private final Set<String> requiredPluginKeys;
    private final Set<String> requiredModuleKeys;
    private final ClassLoader classLoader;

    public ClasspathFilePluginMetadata()
    {
        this(ClasspathFilePluginMetadata.class.getClassLoader());
    }

    ClasspathFilePluginMetadata(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
        this.providedPluginKeys = getStringsFromFile(APPLICATION_PROVIDED_PLUGINS_FILENAME);
        this.requiredPluginKeys = getStringsFromFile(APPLICATION_REQUIRED_PLUGINS_FILENAME);
        this.requiredModuleKeys = getStringsFromFile(APPLICATION_REQUIRED_MODULES_FILENAME);
    }

    public boolean applicationProvided(final Plugin plugin)
    {
        return providedPluginKeys.contains(plugin.getKey());
    }

    public boolean required(final Plugin plugin)
    {
        return requiredPluginKeys.contains(plugin.getKey());
    }

    public boolean required(final ModuleDescriptor<?> module)
    {
        return requiredModuleKeys.contains(module.getCompleteKey());
    }

    public Set<String> getRequiredPluginKeys()
    {
        return requiredPluginKeys;
    }

    public Set<String> getRequiredModuleKeys()
    {
        return requiredModuleKeys;
    }

    private Set<String> getStringsFromFile(final String fileName)
    {
        final ImmutableSet.Builder<String> stringsFromFiles = ImmutableSet.builder();

        final Collection<InputStream> fileInputStreams = getInputStreamsForFilename(fileName);
        try
        {
            for (InputStream fileInputStream : fileInputStreams)
            {
                if (fileInputStream != null)
                {
                    try
                    {
                        @SuppressWarnings ("unchecked")
                        final List<String> lines = readLines(fileInputStream);

                        // Make sure that we trim the strings that we read from the file and filter out comments and blank lines
                        // NOTE: You could use a filter and a transformation but then you need either a private class
                        // or a single enum which causes WAY TOO MUCH debate.
                        for (String line : lines)
                        {
                            final String processedLine = processedLine(line);
                            if (processedLine != null)
                            {
                                stringsFromFiles.add(processedLine);
                            }
                        }
                    }
                    catch (final IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        finally
        {
            for (InputStream fileInputStream : fileInputStreams)
            {
                IOUtils.closeQuietly(fileInputStream);
            }
        }
        return stringsFromFiles.build();
    }

    /**
     * Checks the input to see if it meets the rules we want to enforce about valid lines. If it does not meet the
     * criteria then null is returned, otherwise a trimmed version of the passed in string is returned.
     *
     * @param rawLine the string of data we are processing
     * @return if rawLine does not meet the criteria then null is returned, otherwise a trimmed version of rawLine is
     *         returned.
     */
    private String processedLine(String rawLine)
    {
        if (rawLine == null)
        {
            return null;
        }

        final String trimmedLine = rawLine.trim();
        // Lets not include blank lines
        if (StringUtils.isBlank(trimmedLine))
        {
            return null;
        }

        // Lets not include comments
        if (trimmedLine.startsWith("#"))
        {
            return null;
        }
        return trimmedLine;
    }

    Collection<InputStream> getInputStreamsForFilename(final String fileName)
    {
        final Collection<InputStream> inputStreams = new ArrayList<InputStream>();
        final Class<ClasspathFilePluginMetadata> clazz = ClasspathFilePluginMetadata.class;
        final String resourceName = clazz.getPackage().getName().replace(".", "/") + "/" + fileName;
        try
        {
            final Enumeration<URL> urlEnumeration = classLoader.getResources(resourceName);
            while (urlEnumeration.hasMoreElements())
            {
                inputStreams.add(urlEnumeration.nextElement().openStream());
            }
        }
        catch (final IOException e)
        {
            // Close what we had opened before, one bad apple ruins the batch
            for (InputStream inputStream : inputStreams)
            {
                IOUtils.closeQuietly(inputStream);
            }
            throw new RuntimeException(e);
        }
        return inputStreams;
    }

}
