package com.atlassian.plugin.osgi.loader.transform;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.parsers.XmlDescriptorParser;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.osgi.framework.Constants;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.net.URLClassLoader;
import java.net.URL;

public class DefaultPluginTransformer implements PluginTransformer
{
    static final String ATLASSIAN_PLUGIN_SPRING_XML = "META-INF/spring/atlassian-plugins-spring.xml";
    private static final Logger log = Logger.getLogger(DefaultPluginTransformer.class);

    public File transform(File pluginJar, List<HostComponentRegistration> regs) throws PluginTransformationException
    {
        JarFile jar = null;
        try
        {
            jar = new JarFile(pluginJar);
        } catch (IOException e)
        {
            throw new PluginTransformationException("Plugin is not a valid jar file", e);
        }

        Map<String,byte[]> filesToAdd = new HashMap<String, byte[]>();

        URL atlassianPluginsXmlUrl = null;

        try
        {
            URLClassLoader cl = new URLClassLoader(new URL[]{pluginJar.toURL()});
            atlassianPluginsXmlUrl = cl.getResource(PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
            if (atlassianPluginsXmlUrl == null)
                throw new IllegalStateException("Cannot find atlassian-plugins.xml in jar");

            if (jar.getManifest().getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) == null)
                    filesToAdd.put("META-INF/MANIFEST.MF", generateManifest(atlassianPluginsXmlUrl.openStream(), pluginJar));
        } catch (PluginParseException e)
        {
            throw new PluginTransformationException("Unable to generate manifest", e);
        } catch (IOException e)
        {
            throw new PluginTransformationException("Unable to read existing plugin jar manifest", e);
        }

        if (jar.getEntry(ATLASSIAN_PLUGIN_SPRING_XML) == null) {
            try
            {
                filesToAdd.put(ATLASSIAN_PLUGIN_SPRING_XML, generateSpringXml(atlassianPluginsXmlUrl.openStream(), regs));
            } catch (DocumentException e)
            {
                throw new PluginTransformationException("Unable to generate host component spring XML", e);
            } catch (IOException e)
            {
                throw new PluginTransformationException("Unable to open atlassian-plugins.xml", e);
            }
        }

        try
        {
            return addFilesToExistingZip(pluginJar, filesToAdd);
        } catch (IOException e)
        {
            throw new PluginTransformationException("Unable to add files to plugin jar");
        }

    }

    byte[] generateSpringXml(InputStream in, List<HostComponentRegistration> regs) throws DocumentException
    {
        log.warn("Generating "+ATLASSIAN_PLUGIN_SPRING_XML);
        Document springDoc = DocumentHelper.createDocument();
        Element root = springDoc.addElement("beans");

        root.addNamespace("beans", "http://www.springframework.org/schema/beans");
        root.addNamespace("osgi", "http://www.springframework.org/schema/osgi");
        root.addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.addAttribute(new QName("schemaLocation", new Namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")),
                "http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd\n" +
                "http://www.springframework.org/schema/osgi http://www.springframework.org/schema/osgi/spring-osgi.xsd");
        root.setName("beans:beans");


        // Write plugin components
        SAXReader reader = new SAXReader();
        Document pluginDoc = reader.read(in);
        List<Element> elements = pluginDoc.getRootElement().elements("component");
        for (Element component : elements)
        {
            Element bean = root.addElement("beans:bean");
            bean.addAttribute("id", component.attributeValue("key"));
            bean.addAttribute("alias", component.attributeValue("alias"));
            bean.addAttribute("class", component.attributeValue("class"));
            if ("true".equalsIgnoreCase(component.attributeValue("public")))
            {
                Element osgiService = root.addElement("osgi:service");
                osgiService.addAttribute("id", component.attributeValue("key"));
                osgiService.addAttribute("ref", component.attributeValue("key"));

                List<String> interfaceNames = new ArrayList<String>();
                List<Element> compInterfaces = component.elements("interface");
                for (Element inf : compInterfaces)
                    interfaceNames.add(inf.getTextTrim());

                Element interfaces = osgiService.addElement("osgi:interfaces");
                for (String name : interfaceNames)
                {
                    Element e = interfaces.addElement("beans:value");
                    e.setText(name);
                }
            }
        }

        // write host components
        if (regs != null)
        {
            for (int x=0; x<regs.size(); x++)
            {
                HostComponentRegistration reg = regs.get(x);
                String beanName = reg.getProperties().get("bean-name");
                String id = beanName;
                if (id == null)
                    id = "bean"+x;

                id = id.replaceAll("#", "LB");
                Element osgiService = root.addElement("osgi:reference");
                osgiService.addAttribute("id", id);
                if (beanName != null)
                    osgiService.addAttribute("filter", "(bean-name="+beanName+")");

                Element interfaces = osgiService.addElement("osgi:interfaces");
                for (String name : reg.getMainInterfaces())
                {
                    Element e = interfaces.addElement("beans:value");
                    e.setText(name);
                }
            }
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        OutputFormat format = OutputFormat.createPrettyPrint();

        try
        {
            XMLWriter writer = new XMLWriter(bout, format );
            writer.write(springDoc );
        } catch (IOException e)
        {
            throw new PluginTransformationException("Unable to print generated Spring XML", e);
        }

        System.out.println("Created spring config:"+new String(bout.toByteArray()));
        return bout.toByteArray();
    }

    byte[] generateManifest(InputStream descriptorStream, File file) throws PluginParseException
    {
        log.warn("Generating the manifest");
        Builder builder = new Builder();
        try
        {
            PluginInformationDescriptorParser parser = new PluginInformationDescriptorParser(descriptorStream);
            PluginInformation info = parser.getPluginInformation();

            builder.setJar(file);
            Properties properties = new Properties();

            // Setup defaults
            properties.put("Spring-Context", "*;create-asynchronously:=false");
            properties.put(Analyzer.BUNDLE_SYMBOLICNAME, parser.getKey());
            properties.put(Analyzer.IMPORT_PACKAGE, "*");
            properties.put(Analyzer.EXPORT_PACKAGE, "*");
            properties.put(Analyzer.BUNDLE_VERSION, info.getVersion());

            // remove the verbose Include-Resource entry from generated manifest
            properties.put(Analyzer.REMOVE_HEADERS, Analyzer.INCLUDE_RESOURCE);

            header(properties, Analyzer.BUNDLE_DESCRIPTION, info.getDescription());
            header(properties, Analyzer.BUNDLE_NAME, parser.getKey());
            header(properties, Analyzer.BUNDLE_VENDOR, info.getVendorName());
            header(properties, Analyzer.BUNDLE_DOCURL, info.getVendorUrl());

            builder.setProperties(properties);

            // Not sure if this is the best incantation of bnd, but as I don't have the source, it'll have to do
            builder.calcManifest();
            Jar jar = builder.build();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            jar.writeManifest(bout);
            System.out.println("Created manifest:"+new String(bout.toByteArray()));
            return bout.toByteArray();

        } catch (Exception t)
        {
            throw new PluginParseException("Unable to process plugin to generate OSGi manifest", t);
        }
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
