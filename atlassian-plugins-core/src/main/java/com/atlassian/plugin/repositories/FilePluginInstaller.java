package com.atlassian.plugin.repositories;

import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginInstaller;
import com.atlassian.plugin.RevertablePluginInstaller;
import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File-based implementation of a PluginInstaller which writes plugin artifact
 * to a specified directory.  Handles reverting installs by keeping track of previous
 * installs for a given instance, and backs up all installations automatically.
 *
 * @see RevertablePluginInstaller
 */
public class FilePluginInstaller implements RevertablePluginInstaller
{
    private File directory;
    private static final Logger log = LoggerFactory.getLogger(FilePluginInstaller.class);

    // should be a nice multimap, which we don't see to have
    private final Map<String, List<File>> installedPlugins = CopyOnWriteMap.<String, List<File>>builder().stableViews().newHashMap();

    private static final String BAK_PREFIX = ".bak-";
    private static final Pattern BACKUP_NAME_PATTERN = Pattern.compile("(?:" + BAK_PREFIX + ")*(.*)");
    private static final FilenameFilter BACKUP_NAME_FILTER = new BackupNameFilter();

    /**
     * @param directory where plugin JARs will be installed.
     */
    public FilePluginInstaller(File directory)
    {
        Validate.isTrue(directory != null && directory.exists(), "The plugin installation directory must exist");
        this.directory = directory;
    }

    /**
     * If there is an existing JAR with the same filename, it is replaced.
     *
     * @throws RuntimeException if there was an exception reading or writing files.
     */
    public void installPlugin(String key, PluginArtifact pluginArtifact)
    {
        Validate.notNull(key, "The plugin key must be specified");
        Validate.notNull(pluginArtifact, "The plugin artifact must not be null");

        File newPluginFile = new File(directory, pluginArtifact.getName());
        try
        {
            backup(key, newPluginFile);
            if (newPluginFile.exists())
            {
                // would happen if the plugin was installed for a previous instance
                newPluginFile.delete();
            }
        }
        catch (IOException e)
        {
            log.warn("Unable to backup old file", e);
        }

        OutputStream os = null;
        InputStream in = null;
        try
        {
            os = new FileOutputStream(newPluginFile);
            in = pluginArtifact.getInputStream();
            IOUtils.copy(in, os);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not install plugin: " + pluginArtifact, e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(os);
        }
    }

    /**
     * Reverts an installed plugin.  Handles plugin file overwrites and different names over time.
     *
     * @param pluginKey The plugin key to revert
     * @since 2.5.0
     */
    public void revertInstalledPlugin(String pluginKey)
    {
        List<File> oldFiles = getOldFiles(pluginKey);
        if (!oldFiles.isEmpty())
        {
            File lastBackup = oldFiles.remove(oldFiles.size() - 1);
            String targetFileName = stripPrefix(lastBackup.getName());

            File pluginFile = new File(lastBackup.getParent(), targetFileName);
            if (pluginFile.exists())
            {
                pluginFile.delete();
            }
            try
            {
                FileUtils.moveFile(lastBackup, pluginFile);
            }
            catch (IOException e)
            {
                log.warn("Unable to restore old plugin for " + pluginKey);
            }
        }
    }

    /**
     * Deletes all backup files in the plugin directory
     *
     * @since 2.5.0
     */
    public void clearBackups()
    {
        for (File file : directory.listFiles(BACKUP_NAME_FILTER))
        {
            file.delete();
        }
    }

    private List<File> getOldFiles(String pluginKey)
    {
        List<File> oldFiles = installedPlugins.get(pluginKey);
        if (oldFiles == null)
        {
            oldFiles = new CopyOnWriteArrayList<File>();
            installedPlugins.put(pluginKey, oldFiles);
        }
        return oldFiles;
    }

    private void backup(String pluginKey, File newPluginFile) throws IOException
    {
        List<File> oldFiles = getOldFiles(pluginKey);
        if (newPluginFile.exists())
        {
            File backupFile = findNextBackupFile(newPluginFile);
            FileUtils.moveFile(newPluginFile, backupFile);
            oldFiles.add(backupFile);
        }
        else
        {
            oldFiles.add(newPluginFile);
        }
    }

    private File findNextBackupFile(File newPluginFile)
    {
        File file = newPluginFile;
        do
        {
            file = new File(file.getParent(), BAK_PREFIX + file.getName());
        }
        while (file.exists());

        return file;
    }

    private String stripPrefix(String name)
    {
        Matcher m = BACKUP_NAME_PATTERN.matcher(name);
        if (m.matches())
        {
            return m.group(1);
        }
        throw new RuntimeException("Invalid backup name");
    }

    private static class BackupNameFilter implements FilenameFilter
    {
        public boolean accept(File dir, String name)
        {
            return name.startsWith(BAK_PREFIX);
        }
    }
}
