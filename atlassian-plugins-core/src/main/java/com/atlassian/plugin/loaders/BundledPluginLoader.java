package com.atlassian.plugin.loaders;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.impl.AbstractDelegatingPlugin;
import com.atlassian.plugin.loaders.classloading.Scanner;
import com.atlassian.plugin.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Plugin loader that can find plugins via a single URL, and treats all plugins loaded from
 * the directory as bundled plugins, meaning they can can be upgraded, but not deleted.
 * <p>
 * Depending on the URL:
 * <ul>
 *     <li>If it is a file:// url and represents a directory, all the files in that directory are scanned.</li>
 *     <li>if it is a file:// url and represents a file with a <code>.list</code> suffix, each line in that files
 *     is read as a path to a plugin jar.</li>
 *     <li>Otherwise it assumes the URL is a zip and unzips plugins from it into a local directory,
 *     and ensures that directory only contains plugins from that zip file.  It also</li>
 * </ul>
 *
 */
public class BundledPluginLoader extends ScanningPluginLoader
{
    public BundledPluginLoader(final URL zipUrl, final File pluginPath, final List<PluginFactory> pluginFactories, final PluginEventManager eventManager)
    {
        super(buildScanner(zipUrl, pluginPath), pluginFactories, eventManager);
    }

    @Override
    protected Plugin postProcess(final Plugin plugin)
    {
        return new BundledPluginDelegate(plugin);
    }

    private static Scanner buildScanner(final URL url, final File pluginPath)
    {
        if (url == null)
        {
            throw new IllegalArgumentException("Bundled plugins url cannot be null");
        }

        Scanner scanner = null;

        final File file = FileUtils.toFile(url);

        if (file != null)
        {
            if (file.isDirectory())
            {
                // URL points directly to a directory of jars
                scanner = new DirectoryScanner(file);
            }
            else if (file.isFile() && file.getName().endsWith(".list"))
            {
                // URL points to a file containg a list of jars
                final List<File> files = readListFile(file);
                scanner = new FileListScanner(files);
            }
        }

        if (scanner == null) {
            // default: assume it is a zip
            FileUtils.conditionallyExtractZipFile(url, pluginPath);
            scanner = new DirectoryScanner(pluginPath);
        }

        return scanner;
    }

    private static List<File> readListFile(final File file)
    {
        try
        {
            final List<String> fnames = (List<String>) org.apache.commons.io.FileUtils.readLines(file);
            final List<File> files = new ArrayList<File>();
            for (String fname : fnames)
            {
                files.add(new File(fname));
            }
            return files;
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Unable to read list from " + file, e);
        }
    }


    /**
     * Delegate that overrides methods to enforce bundled plugin behavior
     *
     * @Since 2.2.0
     */
    private static class BundledPluginDelegate extends AbstractDelegatingPlugin
    {

        public BundledPluginDelegate(Plugin delegate)
        {
            super(delegate);
        }

        @Override
        public boolean isBundledPlugin()
        {
            return true;
        }

        @Override
        public boolean isDeleteable()
        {
            return false;
        }
    }
}
