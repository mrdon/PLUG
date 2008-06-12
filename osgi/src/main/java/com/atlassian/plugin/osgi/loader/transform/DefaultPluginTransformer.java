package com.atlassian.plugin.osgi.loader.transform;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.parsers.XmlDescriptorParser;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.osgi.framework.Constants;

import java.io.*;
import java.util.Collection;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DefaultPluginTransformer implements PluginTransformer
{
    public File transform(PluginClassLoader loader, File pluginJar) throws IOException, PluginParseException
    {
        JarFile jar = new JarFile(pluginJar);

        Map<String,byte[]> filesToAdd = new HashMap<String, byte[]>();

        byte[] manifest = null;
        if (jar.getManifest().getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) == null)
            filesToAdd.put("META-INF/MANIFEST.MF", generateManifest(loader, pluginJar));

        /*if (jar.getEntry("META-INF/spring/atlassian-plugin-spring.xml") == null) {

            generateSpringXml();
        }
        */

        return addFilesToExistingZip(pluginJar, filesToAdd);

    }

    byte[] generateManifest(PluginClassLoader loader, File file) throws PluginParseException
    {
        Builder builder = new Builder();
        try
        {
            PluginInformationDescriptorParser parser = new PluginInformationDescriptorParser(loader.getResourceAsStream(PluginManager.PLUGIN_DESCRIPTOR_FILENAME));
            PluginInformation info = parser.getPluginInformation();

            builder.setJar(file);
            Properties properties = new Properties();

            // Setup defaults
            properties.put(Analyzer.BUNDLE_SYMBOLICNAME, parser.getKey());
            properties.put(Analyzer.IMPORT_PACKAGE, "*");
            properties.put(Analyzer.EXPORT_PACKAGE, "*");
            properties.put(Analyzer.BUNDLE_VERSION, info.getVersion());

            // remove the verbose Include-Resource entry from generated manifest
            //properties.put(Analyzer.REMOVE_HEADERS, Analyzer.INCLUDE_RESOURCE);

            header(properties, Analyzer.BUNDLE_DESCRIPTION, info.getDescription());
            header(properties, Analyzer.BUNDLE_NAME, parser.getKey());
            header(properties, Analyzer.BUNDLE_VENDOR, info.getVendorName());
            header(properties, Analyzer.BUNDLE_DOCURL, info.getVendorUrl());

            builder.setProperties(properties);

            builder.calcManifest();
            Jar jar = builder.build();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            jar.writeManifest(bout);
            return bout.toByteArray();
        } catch (Throwable t)
        {
            t.printStackTrace();
        }
        return null;
    }


    public static File addFilesToExistingZip(File zipFile,
			 Map<String,byte[]> files) throws IOException {
                // get a temp file
		File tempFile = File.createTempFile(zipFile.getName(), null);
                // delete it, otherwise you cannot rename your existing zip to it.
		byte[] buf = new byte[1024];

		ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile));

		ZipEntry entry = zin.getNextEntry();
		while (entry != null) {
			String name = entry.getName();
			if (!files.containsKey(name))
            {
				// Add ZIP entry to output stream.
				out.putNextEntry(new ZipEntry(name));
				// Transfer bytes from the ZIP file to the output file
				int len;
				while ((len = zin.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			}
			entry = zin.getNextEntry();
		}
		// Close the streams
		zin.close();
		// Compress the files
		for (Map.Entry<String,byte[]> fentry : files.entrySet())
        {
            InputStream in = new ByteArrayInputStream(fentry.getValue());
			// Add ZIP entry to output stream.
			out.putNextEntry(new ZipEntry(fentry.getKey()));
			// Transfer bytes from the file to the ZIP file
			int len;
			while ((len = in.read(buf)) > 0) {
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


    private static void header(Properties properties, String key, Object value)
    {
        if (value == null)
            return;

        if (value instanceof Collection && ((Collection) value).isEmpty())
            return;

        properties.put(key, value.toString().replaceAll("[\r\n]", ""));
    }


    static class PluginInformationDescriptorParser extends XmlDescriptorParser
    {
        /**
         * @throws com.atlassian.plugin.PluginParseException
         *          if there is a problem reading the descriptor from the XML {@link java.io.InputStream}.
         */
        public PluginInformationDescriptorParser(InputStream source) throws PluginParseException
        {
            super(source);
        }

        public PluginInformation getPluginInformation()
        {
            return createPluginInformation(getDocument().getRootElement().element("plugin-info"));
        }
    }
}
