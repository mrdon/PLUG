package com.atlassian.plugin.osgi.factory.transform.stage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Constants;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;

import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.PluginParseException;
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
    private static final String SPRING_CONTEXT_DEFAULT = "*;timeout:=60";
    static Log log = LogFactory.getLog(GenerateManifestStage.class);

    public void execute(final TransformContext context) throws PluginTransformationException
    {
        final Builder builder = new Builder();
        try
        {
            builder.setJar(context.getPluginFile());

            // We don't care about the modules, so we pass null
            final XmlDescriptorParser parser = new XmlDescriptorParser(context.getDescriptorDocument(), null);

            if (isOsgiBundle(builder.getJar().getManifest()))
            {
                if (context.getExtraImports().isEmpty())
                {
                    final Manifest mf = builder.getJar().getManifest();
                    mf.getMainAttributes().putValue(OsgiPlugin.ATLASSIAN_PLUGIN_KEY, parser.getKey());
                    writeManifestOverride(context, mf);
                    // skip any manifest manipulation by bnd
                    return;
                }
                else
                {
                    // Possibly necessary due to Spring XML creation
                    assertSpringAvailableIfRequired(context);
                    final String imports = addExtraImports(builder.getJar().getManifest().getMainAttributes().getValue(Constants.IMPORT_PACKAGE), context.getExtraImports());
                    builder.setProperty(Constants.IMPORT_PACKAGE, imports);

                    builder.setProperty(OsgiPlugin.ATLASSIAN_PLUGIN_KEY, parser.getKey());
                    builder.mergeManifest(builder.getJar().getManifest());
                }
            }
            else
            {
                final PluginInformation info = parser.getPluginInformation();

                final Properties properties = new Properties();

                // Setup defaults
                if (SpringHelper.isSpringUsed(context))
                {
                    properties.put("Spring-Context", SPRING_CONTEXT_DEFAULT);
                }

                properties.put(Analyzer.BUNDLE_SYMBOLICNAME, parser.getKey());
                properties.put(Analyzer.IMPORT_PACKAGE, "*;resolution:=optional");

                // Don't export anything by default
                //properties.put(Analyzer.EXPORT_PACKAGE, "*");

                properties.put(Analyzer.BUNDLE_VERSION, info.getVersion());

                // remove the verbose Include-Resource entry from generated manifest
                properties.put(Analyzer.REMOVE_HEADERS, Analyzer.INCLUDE_RESOURCE);

                header(properties, Analyzer.BUNDLE_DESCRIPTION, info.getDescription());
                header(properties, Analyzer.BUNDLE_NAME, parser.getKey());
                header(properties, Analyzer.BUNDLE_VENDOR, info.getVendorName());
                header(properties, Analyzer.BUNDLE_DOCURL, info.getVendorUrl());
                header(properties, OsgiPlugin.ATLASSIAN_PLUGIN_KEY, parser.getKey());

                // Scan for embedded jars
                final StringBuilder classpath = new StringBuilder();
                classpath.append(".");
                for (final JarEntry jarEntry : context.getPluginJarEntries())
                {
                    if (jarEntry.getName().startsWith("META-INF/lib/") && jarEntry.getName().endsWith(".jar"))
                    {
                        classpath.append(",").append(jarEntry.getName());
                    }
                }
                header(properties, Analyzer.BUNDLE_CLASSPATH, classpath.toString());

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
            }

            builder.calcManifest();
            builder.getJar().close();
            final Jar jar = builder.build();
            final Manifest mf = jar.getManifest();

            enforceHostVersionsForUnknownImports(mf, context.getSystemExports());

            writeManifestOverride(context, mf);
            jar.close();
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
        return manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) != null;
    }

    private String addExtraImports(String imports, final List<String> extraImports)
    {
        final StringBuilder referrers = new StringBuilder();
        for (final String imp : extraImports)
        {
            referrers.append(imp).append(",");
        }

        if (imports != null && imports.length() > 0)
        {
            imports = referrers + imports;
        }
        else
        {
            imports = referrers.toString();
        }
        return imports;
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

    private static void assertSpringAvailableIfRequired(final TransformContext context)
    {
        if (context.shouldRequireSpring())
        {
            final String header = context.getManifest().getMainAttributes().getValue("Spring-Context");
            if (header == null)
            {
                log.debug("The Spring Manifest header 'Spring-Context' is missing in jar '" +
                        context.getPluginArtifact().toString() + "'.  If you experience any problems, please add it and set it to '" +
                        SPRING_CONTEXT_DEFAULT + "'");
            }
            else if (!header.contains(";timeout:=60"))
            {
                log.warn("The Spring Manifest header in jar '" +  context.getPluginArtifact().toString() + "' isn't " +
                        "set for a 60 second timeout waiting for  dependencies.  Please add ';timeout:=60'");
            }
        }
    }

}
