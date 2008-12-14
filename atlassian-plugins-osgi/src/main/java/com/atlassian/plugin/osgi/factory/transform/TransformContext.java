package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.parsers.XmlDescriptorParser;
import org.codehaus.classworlds.uberjar.protocol.jar.NonLockingJarHandler;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.apache.commons.lang.Validate;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The transform context containing any configuration necessary to enact a JAR transformation
 *
 * @since 2.2.0
 */
public class TransformContext
{
    private final File pluginFile;
    private final JarFile pluginJar;
    private final Manifest manifest;
    private final List<HostComponentRegistration> regs;
    private final Map<String, byte[]> fileOverrides;
    private final Map<String, String> bndInstructions;
    private final List<String> extraImports;
    private final Document descriptorDocument;

    public TransformContext(List<HostComponentRegistration> regs, File pluginFile, String descriptorPath)
    {
        Validate.notNull(pluginFile, "The plugin file must be specified");

        this.regs = regs;
        this.pluginFile = pluginFile;
        try
        {
            this.pluginJar = new JarFile(pluginFile);
            this.manifest = pluginJar.getManifest();
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("File must be a jar", e);
        }
        fileOverrides = new HashMap<String, byte[]>();
        bndInstructions = new HashMap<String, String>();
        this.descriptorDocument = retrievePluginDescriptor(pluginFile, descriptorPath);
        this.extraImports = new ArrayList<String>();
    }

    private static Document retrievePluginDescriptor(File jarFile, String descriptorPath) throws PluginTransformationException
    {
        Document descriptorDocument;
        InputStream descriptorStream = null;
        try
        {
            descriptorStream = retrieveStreamFromJar(jarFile, descriptorPath);
            if (descriptorStream != null)
            {
                DocumentExposingDescriptorParser parser = new DocumentExposingDescriptorParser(descriptorStream);
                descriptorDocument = parser.getDocument();
            }
            else
            {
                DocumentFactory factory = DocumentFactory.getInstance();
                descriptorDocument = factory.createDocument(factory.createElement("atlassian-plugin"));
            }
        }
        catch (IOException e)
        {
            throw new PluginTransformationException("Unable to access descriptor " + descriptorPath, e);
        }
        finally
        {
            IOUtils.closeQuietly(descriptorStream);
        }
        return descriptorDocument;
    }

    public static InputStream retrieveStreamFromJar(File jarFile, String path) throws IOException
    {
        URL atlassianPluginsXmlUrl;
        try
        {
            atlassianPluginsXmlUrl = new URL(new URL("jar:file:" + jarFile.getAbsolutePath() + "!/"), path, NonLockingJarHandler.getInstance());
        }
        catch (MalformedURLException e)
        {
            throw new PluginTransformationException(e);
        }

        try
        {
            return atlassianPluginsXmlUrl.openStream();
        }
        catch (IOException ex)
        {
            // file probably doesn't exist
            return null;
        }
    }

    public File getPluginFile()
    {
        return pluginFile;
    }

    public JarFile getPluginJar()
    {
        return pluginJar;
    }

    public List<HostComponentRegistration> getHostComponentRegistrations()
    {
        return regs;
    }

    public Map<String, byte[]> getFileOverrides()
    {
        return fileOverrides;
    }

    public Map<String, String> getBndInstructions()
    {
        return bndInstructions;
    }

    public Document getDescriptorDocument()
    {
        return descriptorDocument;
    }

    public Manifest getManifest()
    {
        return manifest;
    }

    public List<String> getExtraImports()
    {
        return extraImports;
    }

    private static class DocumentExposingDescriptorParser extends XmlDescriptorParser
    {
        /**
         * @throws com.atlassian.plugin.PluginParseException
         *          if there is a problem reading the descriptor from the XML {@link java.io.InputStream}.
         */
        public DocumentExposingDescriptorParser(InputStream source) throws PluginParseException
        {
            super(source);
        }

        @Override
        public Document getDocument()
        {
            return super.getDocument();
        }
    }
}
