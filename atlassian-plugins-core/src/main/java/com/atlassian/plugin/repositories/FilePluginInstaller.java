package com.atlassian.plugin.repositories;

import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.RevertablePluginInstaller;
import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * File-based implementation of a PluginInstaller which writes plugin artifact
 * to a specified directory.  Handles reverting installs by keeping track of the first installation for a given
 * instance, and restores it.  Installation of plugin artifacts with different names will overwrite an existing artifact
 * of that same name, if it exists, with the only exception being the backup of the first overwritten artifact to
 * support reverting.
 *
 * NOTE: This implementation has a limitation. The issue is that when installing a plugin we are only provided the plugin
 * key and do not know the name of the artifact that provided the original plugin. So if someone installs a new version
 * of an existing plugin in an artifact that has a different name we have no way of telling what artifact provided
 * the original plugin and therefore which artifact to delete. This will result in two of the same plugins, but in
 * different artifacts being left in the plugins directory. Hopefully the versions will differ so that the plugins
 * framework can decide which plugin to enable. 
 *
 * @see RevertablePluginInstaller
 */
public class FilePluginInstaller implements RevertablePluginInstaller
{
    private File directory;
    private static final Logger log = LoggerFactory.getLogger(FilePluginInstaller.class);

    private final Map<String, BackupRepresentation> installedPlugins = CopyOnWriteMap.<String, BackupRepresentation>builder().stableViews().newHashMap();

    public static final String ORIGINAL_PREFIX = ".original-";

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
        checkNotNull(key, "The plugin key must be specified");
        checkNotNull(pluginArtifact, "The plugin artifact must not be null");

        final File newPluginFile = new File(directory, pluginArtifact.getName());
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
        BackupRepresentation backup = installedPlugins.get(pluginKey);
        if (backup != null)
        {
            File currentFile = new File(backup.getBackupFile().getParent(), backup.getCurrentPluginFilename());
            if (currentFile.exists())
            {
                currentFile.delete();
            }

            // We need to copy the original backed-up file back to the original filename if we overwrote the plugin
            // when we first installed it.
            if (backup.isUpgrade())
            {
                try
                {
                    FileUtils.moveFile(backup.getBackupFile(), new File(backup.getBackupFile().getParent(), backup.getOriginalPluginArtifactFilename()));
                }
                catch (IOException e)
                {
                    log.warn("Unable to restore old plugin for " + pluginKey);
                }
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
        for (File file : directory.listFiles(new BackupNameFilter()))
        {
            file.delete();
        }
        installedPlugins.clear();
    }

    private void backup(String pluginKey, File currentPluginArtifact) throws IOException
    {
        BackupRepresentation orig = null;
        // If this is the first time we have seen the pluginkey then we will create a backup representation that may
        // refer to the original plugins file, if the artifact is named the same
        if (!installedPlugins.containsKey(pluginKey))
        {
            orig = getBackupRepresentation(pluginKey, currentPluginArtifact);
        }
        // There is already a backup, we need to delete the intermediate file representation so that we do not
        // leave a bunch of files laying around and update the backup with the new current plugin artifact name
        else
        {
            final BackupRepresentation oldBackupFile = installedPlugins.get(pluginKey);
            // Create a new backup representation that retains the reference to the original backup file but that changes
            // the current plugin artifact name to be the new plugin file representation
            orig = new BackupRepresentation(oldBackupFile, currentPluginArtifact.getName());

            // Delete the previous plugin representation
            final File previousPluginFile = new File(oldBackupFile.getBackupFile().getParent(), oldBackupFile.getCurrentPluginFilename());
            if (previousPluginFile.exists())
            {
                previousPluginFile.delete();
            }
        }

        // Lets keep the backup representation for this plugin up-to-date
        installedPlugins.put(pluginKey, orig);
    }

    private BackupRepresentation getBackupRepresentation(final String pluginKey, final File currentPluginArtifact) throws IOException
    {
        // If there is already a file of the same name as our current plugin artifact then we should create a backup copy
        // of the original file before we overwrite the old plugin file
        if (currentPluginArtifact.exists())
        {
            File backupFile = new File(currentPluginArtifact.getParent(), ORIGINAL_PREFIX + currentPluginArtifact.getName());
            if (backupFile.exists())
            {
                throw new IOException("Existing backup found for plugin " + pluginKey + ".  Cannot install.");
            }

            FileUtils.copyFile(currentPluginArtifact, backupFile);
            return new BackupRepresentation(backupFile, currentPluginArtifact.getName());
        }
        // Since there was no original file then there is not really anything we need to store as a backup
        else
        {
            return new BackupRepresentation(currentPluginArtifact, currentPluginArtifact.getName());
        }
    }

    private static class BackupNameFilter implements FilenameFilter
    {
        public boolean accept(File dir, String name)
        {
            return name.startsWith(ORIGINAL_PREFIX);
        }
    }

    private static class BackupRepresentation
    {
        private final File backupFile;
        private final String originalPluginArtifactFilename;
        private final String currentPluginFilename;
        private final boolean isUpgrade;

        /**
         * @param backupFile the file reference to the file that we should restore if we need to restore a backup
         * @param originalPluginArtifactFilename the name of the original plugin artifact.
         */
        public BackupRepresentation(File backupFile, String originalPluginArtifactFilename)
        {
            this.backupFile = checkNotNull(backupFile, "backupFile");
            this.originalPluginArtifactFilename = checkNotNull(originalPluginArtifactFilename, "originalPluginArtifactFilename");
            this.isUpgrade = !backupFile.getName().equals(originalPluginArtifactFilename);
            this.currentPluginFilename = originalPluginArtifactFilename;
        }

        /**
         * @param oldBackup defines the backup file, original plugin artifact name and if the backup is an "upgrade", non-null.
         * @param currentPluginFilename the name of the current plugin artifact, not null.
         */
        public BackupRepresentation(BackupRepresentation oldBackup, String currentPluginFilename)
        {
            this.backupFile = checkNotNull(oldBackup, "oldBackup").backupFile;
            this.originalPluginArtifactFilename = oldBackup.originalPluginArtifactFilename;
            this.isUpgrade = oldBackup.isUpgrade;
            this.currentPluginFilename = checkNotNull(currentPluginFilename, "currentPluginFilename");
        }

        public File getBackupFile()
        {
            return backupFile;
        }

        public String getOriginalPluginArtifactFilename()
        {
            return originalPluginArtifactFilename;
        }

        public String getCurrentPluginFilename()
        {
            return currentPluginFilename;
        }

        public boolean isUpgrade()
        {
            return isUpgrade;
        }
    }
}
