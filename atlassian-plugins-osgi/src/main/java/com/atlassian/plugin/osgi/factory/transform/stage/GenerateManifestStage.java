package com.atlassian.plugin.osgi.factory.transform.stage;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.parsers.XmlDescriptorParser;
import org.osgi.framework.Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

/**
 * Generates an OSGi manifest if not already defined.  Should be the last stage.
 *
 * @since 2.2.0
 */
public class GenerateManifestStage implements TransformStage
{
    public void execute(TransformContext context) throws PluginTransformationException
    {
        Builder builder = new Builder();
        try
        {
            builder.setJar(context.getPluginFile());

            // Possibly necessary due to Spring XML creation
            if (isOsgiBundle(builder.getJar().getManifest()))
            {
                String imports = addExtraImports(builder.getJar().getManifest().getMainAttributes().getValue(Constants.IMPORT_PACKAGE), context.getExtraImports());
                builder.setProperty(Constants.IMPORT_PACKAGE, imports);
                builder.mergeManifest(builder.getJar().getManifest());
            }
            else
            {
                XmlDescriptorParser parser = new XmlDescriptorParser(context.getDescriptorDocument());
                PluginInformation info = parser.getPluginInformation();

                Properties properties = new Properties();

                // Setup defaults
                properties.put("Spring-Context", "*;timeout:=60");
                properties.put(Analyzer.BUNDLE_SYMBOLICNAME, parser.getKey());
                properties.put(Analyzer.IMPORT_PACKAGE, "*;resolution:=optional");
                properties.put(Analyzer.EXPORT_PACKAGE, "*");
                properties.put(Analyzer.BUNDLE_VERSION, info.getVersion());

                // remove the verbose Include-Resource entry from generated manifest
                properties.put(Analyzer.REMOVE_HEADERS, Analyzer.INCLUDE_RESOURCE);

                header(properties, Analyzer.BUNDLE_DESCRIPTION, info.getDescription());
                header(properties, Analyzer.BUNDLE_NAME, parser.getKey());
                header(properties, Analyzer.BUNDLE_VENDOR, info.getVendorName());
                header(properties, Analyzer.BUNDLE_DOCURL, info.getVendorUrl());

                // Scan for embedded jars
                StringBuilder classpath = new StringBuilder();
                classpath.append(".");
                for (Enumeration<JarEntry> e = context.getPluginJar().entries(); e.hasMoreElements();)
                {
                    JarEntry entry = e.nextElement();
                    if (entry.getName().startsWith("META-INF/lib/") && entry.getName().endsWith(".jar"))
                    {
                        classpath.append(",").append(entry.getName());
                    }
                }
                header(properties, Analyzer.BUNDLE_CLASSPATH, classpath.toString());

                // Process any bundle instructions in atlassian-plugin.xml
                properties.putAll(context.getBndInstructions());

                // Add extra imports to the imports list
                properties.put(Analyzer.IMPORT_PACKAGE, addExtraImports(properties.getProperty(Analyzer.IMPORT_PACKAGE), context.getExtraImports()));
                builder.setProperties(properties);
            }

            builder.calcManifest();
            Jar jar = builder.build();
            Manifest mf = jar.getManifest();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            mf.write(bout);
            context.getFileOverrides().put("META-INF/MANIFEST.MF", bout.toByteArray());
        }
        catch (Exception t)
        {
            throw new PluginParseException("Unable to process plugin to generate OSGi manifest", t);
        }
    }

    private boolean isOsgiBundle(Manifest manifest) throws IOException
    {
        return manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) != null;
    }

    private String addExtraImports(String imports, List<String> extraImports)
    {
        StringBuilder referrers = new StringBuilder();
        for (String imp : extraImports)
        {
            referrers.append(imp).append(",");
        }

        if (imports != null && imports.length() > 0)
        {
            imports = referrers + imports;
        }
        else
        {
            imports = referrers.substring(0, referrers.length() - 1);
        }
        return imports;
    }

    private static void header(Properties properties, String key, Object value)
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

}
