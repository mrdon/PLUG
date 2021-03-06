package com.atlassian.plugin.osgi.factory.transform;

import static com.atlassian.plugin.osgi.factory.transform.JarUtils.getEntries;
import static com.atlassian.plugin.osgi.factory.transform.JarUtils.getEntry;

import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.factory.transform.model.ComponentImport;
import com.atlassian.plugin.osgi.factory.transform.model.SystemExports;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.parsers.XmlDescriptorParser;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.dom4j.Document;
import org.dom4j.Element;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

/**
 * The transform context containing any configuration necessary to enact a JAR transformation.
 * 
 * @since 2.2.0
 */
public final class TransformContext
{
    private final Manifest manifest;
    private final List<HostComponentRegistration> regs;
    private final Map<String, byte[]> fileOverrides;
    private final Map<String, String> bndInstructions;
    private final Document descriptorDocument;
    private final List<String> extraImports;
    private final List<String> extraExports;
    private final Set<String> bundleClassPathJars;
    private final PluginArtifact pluginArtifact;
    private final Map<String, ComponentImport> componentImports;
    private final SystemExports systemExports;
    private final Set<String> applicationKeys;
    private boolean shouldRequireSpring = false;
    private final OsgiContainerManager osgiContainerManager;
    private final Set<HostComponentRegistration> requiredHostComponents;

    private static final Logger LOG = LoggerFactory.getLogger(TransformContext.class);

    // The transformation is mainly about generating spring beans.
    // We don't want to have name conflicts between them. This map helps keep track of that.
    // The definition of this map is "beanName -> source".
    private final Map<String, String> beanSourceMap = new HashMap<String, String>();

    public TransformContext(final List<HostComponentRegistration> regs, final SystemExports systemExports, final PluginArtifact pluginArtifact, final Set<String> applicationKeys, final String descriptorPath, final OsgiContainerManager osgiContainerManager)
    {
        this.osgiContainerManager = osgiContainerManager;
        Validate.notNull(pluginArtifact, "The plugin artifact must be specified");
        Validate.notNull(descriptorPath, "The plugin descriptor path must be specified");
        Validate.notNull(systemExports, "The system exports must be specified");

        this.regs = regs;
        this.systemExports = systemExports;
        this.pluginArtifact = pluginArtifact;
        this.applicationKeys = (applicationKeys == null ? Collections.<String> emptySet() : applicationKeys);

        manifest = JarUtils.getManifest(pluginArtifact.toFile());
        fileOverrides = new HashMap<String, byte[]>();
        bndInstructions = new HashMap<String, String>();
        descriptorDocument = retrieveDocFromJar(pluginArtifact, descriptorPath);
        extraImports = new ArrayList<String>();
        extraExports = new ArrayList<String>();
        bundleClassPathJars = new HashSet<String>();

        componentImports = parseComponentImports(descriptorDocument);
        requiredHostComponents = new HashSet<HostComponentRegistration>();
    }

    public File getPluginFile()
    {
        return pluginArtifact.toFile();
    }

    public PluginArtifact getPluginArtifact()
    {
        return pluginArtifact;
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

    public void addBundleClasspathJar(String classpath)
    {
        bundleClassPathJars.add(classpath);
    }

    public Set<String> getBundleClassPathJars()
    {
        return Collections.unmodifiableSet(bundleClassPathJars);
    }

    public Map<String, ComponentImport> getComponentImports()
    {
        return componentImports;
    }

    public SystemExports getSystemExports()
    {
        return systemExports;
    }

    public Set<String> getApplicationKeys()
    {
        return applicationKeys;
    }

    public boolean shouldRequireSpring()
    {
        return shouldRequireSpring;
    }

    public void setShouldRequireSpring(final boolean shouldRequireSpring)
    {
        this.shouldRequireSpring = shouldRequireSpring;
        if (shouldRequireSpring)
        {
            getFileOverrides().put("META-INF/spring/", new byte[0]);
        }
    }

    public OsgiContainerManager getOsgiContainerManager()
    {
        return osgiContainerManager;
    }

    public Iterable<JarEntry> getPluginJarEntries()
    {
        return getEntries(pluginArtifact.toFile());
    }

    public JarEntry getPluginJarEntry(final String path)
    {
        return getEntry(pluginArtifact.toFile(), path);
    }

    public void addRequiredHostComponent(final HostComponentRegistration hostComponent)
    {
        requiredHostComponents.add(hostComponent);
    }

    public Set<HostComponentRegistration> getRequiredHostComponents()
    {
        return requiredHostComponents;
    }

    /**
     * Track a bean by remembering its name and source.
     * If there is already a bean with the same name, {@link PluginTransformationException} will be raised.
     *
     * @param name id, name, or alias of the bean = basically any names which can be used to refer to the bean in Spring context.
     * @param source the source of the bean.
     */
    public void trackBean(final String name, final String source) throws PluginTransformationException
    {
        Validate.notNull(name, "empty bean name");
        Validate.notNull(source, "source of bean is required");

        // if it already exists, just explode.
        if (beanSourceMap.containsKey(name))
        {
            final String message = String.format("The bean identifier '%s' is used by two different beans from %s and %s."
                                                 + "This is a bad practice and may not be supported in newer plugin framework version.",
                                                 name, source, beanSourceMap.get(name));
            // TODO: turn this warning into exception in the future. PLUG-682
            LOG.warn(message);
            //throw new PluginTransformationException(message);
        }

        // otherwise, just track it.
        beanSourceMap.put(name, source);
    }

    /**
     * Check if the bean has been tracked.
     * This is used for testing only.
     *
     * @param name the bean name.
     */
    public boolean beanExists(final String name)
    {
        return beanSourceMap.containsKey(name);
    }

    //
    // static utils
    //

    private static Map<String, ComponentImport> parseComponentImports(final Document descriptorDocument)
    {
        final Map<String, ComponentImport> componentImports = new HashMap<String, ComponentImport>();
        @SuppressWarnings("unchecked")
        final List<Element> elements = descriptorDocument.getRootElement().elements("component-import");
        for (final Element component : elements)
        {
            final ComponentImport ci = new ComponentImport(component);
            componentImports.put(ci.getKey(), ci);
        }
        return ImmutableMap.copyOf(componentImports);
    }

    private static Document retrieveDocFromJar(final PluginArtifact pluginArtifact, final String descriptorPath) throws PluginTransformationException
    {
        InputStream stream = null;
        try
        {
            stream = pluginArtifact.getResourceAsStream(descriptorPath);
            if (stream == null)
            {
                throw new PluginTransformationException("Unable to access descriptor " + descriptorPath);
            }
            return new DocumentExposingDescriptorParser(stream).getDocument();
        }
        finally
        {
            IOUtils.closeQuietly(stream);
        }
    }

    /**
     * Get the document (protected method on super-class).
     */
    private static class DocumentExposingDescriptorParser extends XmlDescriptorParser
    {
        /**
         * @throws com.atlassian.plugin.PluginParseException
         *          if there is a problem reading the descriptor from the XML {@link java.io.InputStream}.
         */
        DocumentExposingDescriptorParser(final InputStream source) throws PluginParseException
        {
            // A null application key is fine here as we are only interested in the parsed document
            super(source, (String) null);
        }

        @Override
        public Document getDocument()
        {
            return super.getDocument();
        }
    }
}
