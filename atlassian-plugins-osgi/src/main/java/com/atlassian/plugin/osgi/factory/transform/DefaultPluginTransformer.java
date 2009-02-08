package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.factory.transform.stage.*;
import com.atlassian.plugin.osgi.container.OsgiPersistentCache;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.PluginArtifact;
import org.apache.commons.lang.Validate;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Default implementation of plugin transformation that uses stages to convert a plain JAR into an OSGi bundle.
 */
public class DefaultPluginTransformer implements PluginTransformer
{
    private static final Logger log = Logger.getLogger(DefaultPluginTransformer.class);

    private final String pluginDescriptorPath;
    private final List<TransformStage> stages;
    private final File bundleCache;
    /**
     * Constructs a transformer with the default stages
     *
     * @param cache The OSGi cache configuration for transformed plugins
     * @param pluginDescriptorPath The path to the plugin descriptor
     * @since 2.2.0
     */
    public DefaultPluginTransformer(OsgiPersistentCache cache, String pluginDescriptorPath)
    {
        this(cache, pluginDescriptorPath, new ArrayList<TransformStage>()
        {{
            add(new AddBundleOverridesStage());
            add(new ComponentImportSpringStage());
            add(new ComponentSpringStage());
            add(new HostComponentSpringStage());
            add(new ModuleTypeSpringStage());
            add(new GenerateManifestStage());
        }});
    }

    /**
     * Constructs a transformer and its stages
     *
     * @param cache The OSGi cache configuration for transformed plugins
     * @param pluginDescriptorPath The descriptor path
     * @param stages A set of stages
     * @since 2.2.0
     */
    public DefaultPluginTransformer(OsgiPersistentCache cache, String pluginDescriptorPath, List<TransformStage> stages)
    {
        Validate.notNull(pluginDescriptorPath, "The plugin descriptor path is required");
        Validate.notNull(stages, "A list of stages is required");
        this.pluginDescriptorPath = pluginDescriptorPath;
        this.stages = Collections.unmodifiableList(new ArrayList<TransformStage>(stages));
        this.bundleCache = cache.getTransformedPluginCache();
        this.bundleCache.mkdir();

    }

    /**
     * Transforms the file into an OSGi bundle
     *
     * @param pluginJar The plugin jar
     * @param regs      The list of registered host components
     * @return The new OSGi-enabled plugin jar
     * @throws PluginTransformationException If anything goes wrong
     */
    public File transform(File pluginJar, List<HostComponentRegistration> regs) throws PluginTransformationException
    {
        return transform(new JarPluginArtifact(pluginJar), regs);
    }

    /**
     * Transforms the file into an OSGi bundle
     *
     * @param pluginArtifact The plugin artifact, usually a jar
     * @param regs      The list of registered host components
     * @return The new OSGi-enabled plugin jar
     * @throws PluginTransformationException If anything goes wrong
     */
    public File transform(PluginArtifact pluginArtifact, List<HostComponentRegistration> regs) throws PluginTransformationException
    {
        Validate.notNull(pluginArtifact, "The plugin artifact is required");
        Validate.notNull(regs, "The host component registrations are required");

        File artifactFile = pluginArtifact.toFile();

        // Look in cache first
        File cachedPlugin = getFromCache(artifactFile);
        if (cachedPlugin != null)
        {
            return cachedPlugin;
        }

        TransformContext context = new TransformContext(regs, pluginArtifact, pluginDescriptorPath);
        for (TransformStage stage : stages)
        {
            stage.execute(context);
        }

        // Create a new jar by overriding the specified files
        try
        {
            if (log.isDebugEnabled())
            {
                StringBuilder sb = new StringBuilder();
                sb.append("Overriding files in ").append(pluginArtifact.toString()).append(":\n");
                for (Map.Entry<String, byte[]> entry : context.getFileOverrides().entrySet())
                {
                    sb.append("==").append(entry.getKey()).append("==\n");
                    sb.append(new String(entry.getValue()));
                }
                log.debug(sb.toString());
            }
            return addFilesToExistingZip(artifactFile, context.getFileOverrides());
        }
        catch (IOException e)
        {
            throw new PluginTransformationException("Unable to add files to plugin jar");
        }
    }

    private File getFromCache(File artifact)
    {
        String name = generateCacheName(artifact);
        for (File child : bundleCache.listFiles())
        {
            if (child.getName().equals(name))
                return child;
        }
        return null;
    }

    private static String generateCacheName(File file)
    {
        return file.getName()+"_"+file.lastModified();
    }


    /**
     * Creates a new jar by overriding the specified files in the existing one
     *
     * @param zipFile The existing zip file
     * @param files   The files to override
     * @return The new zip
     * @throws IOException If there are any problems processing the streams
     */
    File addFilesToExistingZip(File zipFile,
                                      Map<String, byte[]> files) throws IOException
    {
        // get a temp file
        File tempFile = new File(bundleCache, generateCacheName(zipFile));
        // delete it, otherwise you cannot rename your existing zip to it.
        byte[] buf = new byte[1024];

        ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile));

        ZipEntry entry = zin.getNextEntry();
        while (entry != null)
        {
            String name = entry.getName();
            if (!files.containsKey(name))
            {
                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry(name));
                // Transfer bytes from the ZIP file to the output file
                int len;
                while ((len = zin.read(buf)) > 0)
                    out.write(buf, 0, len);
            }
            entry = zin.getNextEntry();
        }
        // Close the streams
        zin.close();
        // Compress the files
        for (Map.Entry<String, byte[]> fentry : files.entrySet())
        {
            InputStream in = new ByteArrayInputStream(fentry.getValue());
            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(fentry.getKey()));
            // Transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0)
            {
                out.write(buf, 0, len);
            }
            // Complete the entry
            out.closeEntry();
            in.close();
        }
        // Complete the ZIP file
        out.close();
        return tempFile;
    }

    
}
