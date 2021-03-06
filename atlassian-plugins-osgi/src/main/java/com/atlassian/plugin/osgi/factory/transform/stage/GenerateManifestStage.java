package com.atlassian.plugin.osgi.factory.transform.stage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;

import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.osgi.factory.transform.model.SystemExports;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import com.atlassian.plugin.parsers.XmlDescriptorParser;

/**
 * Generates an OSGi manifest if not already defined.  Should be the last stage.
 *
 * @since 2.2.0
 */
public class GenerateManifestStage implements TransformStage
{
    private final int SPRING_TIMEOUT = PluginUtils.getDefaultEnablingWaitPeriod();
    private final String SPRING_CONTEXT_DEFAULT = "*;"+ SPRING_CONTEXT_TIMEOUT + SPRING_TIMEOUT;
    static Logger log = LoggerFactory.getLogger(GenerateManifestStage.class);
    public static final String SPRING_CONTEXT = "Spring-Context";
    private static final String SPRING_CONTEXT_TIMEOUT = "timeout:=";
    private static final String SPRING_CONTEXT_DELIM = ";";
    private static final String RESOLUTION_DIRECTIVE = "resolution:";

    public void execute(final TransformContext context) throws PluginTransformationException
    {
        final Builder builder = new Builder();
        try
        {
            builder.setJar(context.getPluginFile());

            // We don't care about the modules, so we pass null
            final XmlDescriptorParser parser = new XmlDescriptorParser(context.getDescriptorDocument());

            Manifest mf;
            if (isOsgiBundle(context.getManifest()))
            {
                if (context.getExtraImports().isEmpty())
                {
                    boolean modified = false;
                    mf = builder.getJar().getManifest();
                    for (Map.Entry<String,String> entry : getRequiredOsgiHeaders(context, parser.getKey()).entrySet())
                    {
                        if (manifestDoesntHaveRequiredOsgiHeader(mf, entry))
                        {
                            mf.getMainAttributes().putValue(entry.getKey(), entry.getValue());
                            modified = true;
                        }
                    }
                    validateOsgiVersionIsValid(mf);
                    if (modified)
                    {
                        writeManifestOverride(context, mf);
                    }
                    // skip any manifest manipulation by bnd
                    return;
                }
                else
                {
                    // Possibly necessary due to Spring XML creation

                    assertSpringAvailableIfRequired(context);
                    mf = builder.getJar().getManifest();
                    final String imports = addExtraImports(builder.getJar().getManifest().getMainAttributes().getValue(Constants.IMPORT_PACKAGE), context.getExtraImports());
                    mf.getMainAttributes().putValue(Constants.IMPORT_PACKAGE, imports);

                    for (Map.Entry<String,String> entry : getRequiredOsgiHeaders(context, parser.getKey()).entrySet())
                    {
                        mf.getMainAttributes().putValue(entry.getKey(), entry.getValue());
                    }
                }
            }
            else
            {
                final PluginInformation info = parser.getPluginInformation();

                final Properties properties = new Properties();

                // Setup defaults
                for (Map.Entry<String,String> entry : getRequiredOsgiHeaders(context, parser.getKey()).entrySet())
                {
                    properties.put(entry.getKey(), entry.getValue());
                }

                properties.put(Analyzer.BUNDLE_SYMBOLICNAME, parser.getKey());
                properties.put(Analyzer.IMPORT_PACKAGE, "*;resolution:=optional");

                // Don't export anything by default
                //properties.put(Analyzer.EXPORT_PACKAGE, "*");

                properties.put(Analyzer.BUNDLE_VERSION, info.getVersion());

                // remove the verbose Include-Resource entry from generated manifest
                properties.put(Analyzer.REMOVEHEADERS, Analyzer.INCLUDE_RESOURCE);

                header(properties, Analyzer.BUNDLE_DESCRIPTION, info.getDescription());
                header(properties, Analyzer.BUNDLE_NAME, parser.getKey());
                header(properties, Analyzer.BUNDLE_VENDOR, info.getVendorName());
                header(properties, Analyzer.BUNDLE_DOCURL, info.getVendorUrl());

                List<String> bundleClassPaths = new ArrayList<String>();

                // the jar root.
                bundleClassPaths.add(".");

                // inner jars. make the order deterministic here.
                List<String> innerClassPaths = new ArrayList<String>(context.getBundleClassPathJars());
                Collections.sort(innerClassPaths);
                bundleClassPaths.addAll(innerClassPaths);

                // generate bundle classpath.
                header(properties, Analyzer.BUNDLE_CLASSPATH, StringUtils.join(bundleClassPaths, ','));

                // Process any bundle instructions in atlassian-plugin.xml
                properties.putAll(context.getBndInstructions());

                // Add extra imports to the imports list
                properties.put(Analyzer.IMPORT_PACKAGE, addExtraImports(properties.getProperty(Analyzer.IMPORT_PACKAGE), context.getExtraImports()));

                // Add extra exports to the exports list
                if (!properties.containsKey(Analyzer.EXPORT_PACKAGE))
                {
                    properties.put(Analyzer.EXPORT_PACKAGE, StringUtils.join(context.getExtraExports(), ','));
                }
                builder.setProperties(properties);
                builder.calcManifest();
                builder.getJar().close();
                Jar jar = null;
                try
                {
                    jar = builder.build();
                    mf = jar.getManifest();
                }
                finally
                {
                    if (jar != null)
                    {
                        jar.close();
                    }
                }
            }

            enforceHostVersionsForUnknownImports(mf, context.getSystemExports());
            validateOsgiVersionIsValid(mf);

            writeManifestOverride(context, mf);
        }
        catch (final Exception t)
        {
            throw new PluginParseException("Unable to process plugin to generate OSGi manifest", t);
        } finally
        {
            builder.getJar().close();
            builder.close();
        }

    }

    private Map<String,String> getRequiredOsgiHeaders(TransformContext context, String pluginKey)
    {
        Map<String, String> props = new HashMap<String, String>();
        props.put(OsgiPlugin.ATLASSIAN_PLUGIN_KEY, pluginKey);
        String springHeader = getDesiredSpringContextValue(context);
        if (springHeader != null)
        {
            props.put(SPRING_CONTEXT, springHeader);
        }
        return props;
    }

    private String getDesiredSpringContextValue(TransformContext context)
    {
        // Check for the explicit context value
        final String header = context.getManifest().getMainAttributes().getValue(SPRING_CONTEXT);
        if (header != null)
        {
            return ensureDefaultTimeout(header);
        }

        // Check for the spring files, as the default header value looks here
        // TODO: This is probly not correct since if there is no META-INF/spring/*.xml, it's still not spring-powered.
        if (context.getPluginArtifact().doesResourceExist("META-INF/spring/") ||
            context.shouldRequireSpring() ||
            context.getDescriptorDocument() != null)
        {
            return SPRING_CONTEXT_DEFAULT;
        }
        return null;
    }

    private String ensureDefaultTimeout(final String header)
    {
        final boolean noTimeOutSpecified = StringUtils.isEmpty(System.getProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT));

        if (noTimeOutSpecified)
        {
            return header;
        }
        else
        {
            StringBuilder headerBuf;
            //Override existing timeout
            if (header.contains(SPRING_CONTEXT_TIMEOUT))
            {
                StringTokenizer tokenizer = new StringTokenizer(header, SPRING_CONTEXT_DELIM);
                headerBuf = new StringBuilder();
                while (tokenizer.hasMoreElements())
                {
                    String directive = (String) tokenizer.nextElement();
                    if (directive.startsWith(SPRING_CONTEXT_TIMEOUT))
                    {
                        directive = SPRING_CONTEXT_TIMEOUT + SPRING_TIMEOUT;
                    }
                    headerBuf.append(directive);
                    if (tokenizer.hasMoreElements())
                    {
                        headerBuf.append(SPRING_CONTEXT_DELIM);
                    }
                }
            }
            else
            {
                //Append new timeout
                headerBuf = new StringBuilder(header);
                headerBuf.append(SPRING_CONTEXT_DELIM + SPRING_CONTEXT_TIMEOUT + SPRING_TIMEOUT);
            }
            return headerBuf.toString();
        }
    }

    private void validateOsgiVersionIsValid(Manifest mf)
    {
        String version = mf.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
        try
        {
            if (Version.parseVersion(version) == Version.emptyVersion)
            {
                // we still consider an empty version to be bad
                throw new IllegalArgumentException();
            }
        }
        catch (IllegalArgumentException ex)
        {
            throw new IllegalArgumentException("Plugin version '" + version + "' is required and must be able to be " +
                    "parsed as an OSGi version - MAJOR.MINOR.MICRO.QUALIFIER");
        }
    }

    private void writeManifestOverride(final TransformContext context, final Manifest mf)
            throws IOException
    {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        mf.write(bout);
        context.getFileOverrides().put("META-INF/MANIFEST.MF", bout.toByteArray());
    }

    /**
     * Scans for any imports with no version specified and locks them into the specific version exported by the host
     * container
     * @param manifest The manifest to read and manipulate
     * @param exports The list of host exports
     */
    private void enforceHostVersionsForUnknownImports(final Manifest manifest, final SystemExports exports)
    {
        final String origImports = manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
        if (origImports != null)
        {
            final StringBuilder imports = new StringBuilder();
            final Map<String,Map<String,String>> header = OsgiHeaderUtil.parseHeader(origImports);
            for (final Map.Entry<String,Map<String,String>> pkgImport : header.entrySet())
            {
                String imp = null;
                if (pkgImport.getValue().isEmpty())
                {
                    final String export = exports.getFullExport(pkgImport.getKey());
                    if (!export.equals(imp))
                    {
                        imp = export;
                    }

                }
                if (imp == null)
                {
                    imp = OsgiHeaderUtil.buildHeader(pkgImport.getKey(), pkgImport.getValue());
                }
                imports.append(imp);
                imports.append(",");
            }
            if (imports.length() > 0)
            {
                imports.deleteCharAt(imports.length() - 1);
            }

            manifest.getMainAttributes().putValue(Constants.IMPORT_PACKAGE, imports.toString());
        }
    }

    private boolean isOsgiBundle(final Manifest manifest) throws IOException
    {
        // OSGi core spec 4.2 section 3.5.2: The Bundle-SymbolicName manifest header is a mandatory header.
        return manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) != null;
    }

    private String addExtraImports(String importsLine, final List<String> extraImports)
    {
        Map<String,Map<String,String>> originalImports = OsgiHeaderUtil.parseHeader(importsLine);
        for (String exImport : extraImports)
        {
            if (!exImport.startsWith("java."))
            {
                // the extraImportPackage here can be in the form 'package;version=blah'. We only use the package component to check if it's already required.
                final String extraImportPackage = StringUtils.split(exImport, ';')[0];

                Map attrs = originalImports.get(extraImportPackage);
                // if the package is already required by the import directive supplied by plugin developer, we use the supplied one.
                if (attrs != null)
                {
                    Object resolution = attrs.get(RESOLUTION_DIRECTIVE);
                    if (Constants.RESOLUTION_OPTIONAL.equals(resolution))
                    {
                        attrs.put(RESOLUTION_DIRECTIVE, Constants.RESOLUTION_MANDATORY);
                    }
                }
                // otherwise, it is system determined.
                else
                {
                    originalImports.put(exImport, Collections.<String, String>emptyMap());
                }
            }
        }

        return OsgiHeaderUtil.buildHeader(originalImports);
    }

    private boolean manifestDoesntHaveRequiredOsgiHeader(Manifest mf, Entry<String, String> entry)
    {
        if (mf.getMainAttributes().containsKey(new Attributes.Name(entry.getKey())))
        {
            return !entry.getValue().equals(mf.getMainAttributes().getValue(entry.getKey()));
        }
        return true;
    }

    private static void header(final Properties properties, final String key, final Object value)
    {
        if (value == null)
        {
            return;
        }

        if (value instanceof Collection && ((Collection) value).isEmpty())
        {
            return;
        }

        properties.put(key, value.toString().replaceAll("[\r\n]", ""));
    }

    private void assertSpringAvailableIfRequired(final TransformContext context)
    {
        if (context.shouldRequireSpring())
        {
            final String header = context.getManifest().getMainAttributes().getValue(SPRING_CONTEXT);
            if (header == null)
            {
                log.debug("The Spring Manifest header 'Spring-Context' is missing in jar '" +
                        context.getPluginArtifact().toString() + "'.  If you experience any problems, please add it and set it to '" +
                        SPRING_CONTEXT_DEFAULT + "'");
            }
            else if (!header.contains(";timeout:=" + SPRING_TIMEOUT))
            {
                log.warn("The Spring Manifest header in jar '" +  context.getPluginArtifact().toString() + "' isn't " +
                        "set for a " + SPRING_TIMEOUT + " second timeout waiting for  dependencies.  " +
                        "Please add ';timeout:=" + SPRING_TIMEOUT + "'");
            }
        }
    }

}
