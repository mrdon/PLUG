package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.factory.transform.model.ComponentImport;
import com.atlassian.plugin.parsers.XmlDescriptorParser;
import org.codehaus.classworlds.uberjar.protocol.jar.NonLockingJarHandler;
import org.dom4j.Document;
import org.dom4j.Element;
import org.apache.commons.lang.Validate;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The transform context containing any configuration necessary to enact a JAR transformation
 *
 * @since 2.2.0
 */
public class TransformContext
{
    private final JarFile pluginJar;
    private final Manifest manifest;
    private final List<HostComponentRegistration> regs;
    private final Map<String, byte[]> fileOverrides;
    private final Map<String, String> bndInstructions;
    private final Document descriptorDocument;
    private final List<String> extraImports;
    private final List<String> extraExports;
    private final PluginArtifact pluginArtifact;
    private final Map<String, ComponentImport> componentImports;

    public TransformContext(List<HostComponentRegistration> regs, PluginArtifact pluginArtifact, String descriptorPath)
    {
        Validate.notNull(pluginArtifact, "The plugin artifact must be specified");
        Validate.notNull(descriptorPath, "The plugin descriptor path must be specified");

        this.regs = regs;
        this.pluginArtifact = pluginArtifact;
        try
        {
            this.pluginJar = new JarFile(pluginArtifact.toFile());
            this.manifest = pluginJar.getManifest();
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("File must be a jar", e);
        }
        fileOverrides = new HashMap<String, byte[]>();
        bndInstructions = new HashMap<String, String>();
        this.descriptorDocument = retrieveDocFromJar(pluginArtifact, descriptorPath);
        this.extraImports = new ArrayList<String>();
        this.extraExports = new ArrayList<String>();

        this.componentImports = Collections.unmodifiableMap(parseComponentImports(descriptorDocument));
    }

    private Map<String, ComponentImport> parseComponentImports(Document descriptorDocument)
    {
        Map<String,ComponentImport> componentImports = new HashMap<String,ComponentImport>();
        List<Element> elements = descriptorDocument.getRootElement().elements("component-import");
        for (Element component : elements)
        {
            ComponentImport ci = new ComponentImport(component);
            componentImports.put(ci.getKey(), ci);
        }
        return componentImports;
    }

    private Document retrieveDocFromJar(PluginArtifact pluginArtifact, String descriptorPath) throws PluginTransformationException
    {
        Document descriptorDocument;
        InputStream descriptorStream = null;
        try
        {
            descriptorStream = pluginArtifact.getResourceAsStream(descriptorPath);
            if (descriptorStream == null)
            {
                throw new PluginTransformationException("Unable to access descriptor " + descriptorPath);
            }
            DocumentExposingDescriptorParser parser = new DocumentExposingDescriptorParser(descriptorStream);
            descriptorDocument = parser.getDocument();
        }
        finally
        {
            IOUtils.closeQuietly(descriptorStream);
        }
        return descriptorDocument;
    }

    public File getPluginFile()
    {
        return pluginArtifact.toFile();
    }

    public PluginArtifact getPluginArtifact()
    {
        return pluginArtifact;
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

    public List<String> getExtraExports()
    {
        return extraExports;
    }

    public Map<String, ComponentImport> getComponentImports()
    {
        return componentImports;
    }

    private static class DocumentExposingDescriptorParser extends XmlDescriptorParser
    {
        /**
         * @throws com.atlassian.plugin.PluginParseException
         *          if there is a problem reading the descriptor from the XML {@link java.io.InputStream}.
         */
        public DocumentExposingDescriptorParser(InputStream source) throws PluginParseException
        {
            // A null application key is fine here as we are only interested in the parsed document
            super(source, null);
        }

        @Override
        public Document getDocument()
        {
            return super.getDocument();
        }
    }
}
