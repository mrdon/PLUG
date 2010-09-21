package com.atlassian.plugin.osgi.container.felix;

import com.atlassian.plugin.util.ClassLoaderUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.twdata.pkgscanner.DefaultOsgiVersionConverter;
import org.twdata.pkgscanner.ExportPackage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

final class ExportBuilderUtils
{
    private static Logger LOG = LoggerFactory.getLogger(ExportBuilderUtils.class);
    private static final DefaultOsgiVersionConverter converter = new DefaultOsgiVersionConverter();

    /**
     * Not for instantiation.
     */
    private ExportBuilderUtils()
    {}

    /**
     * Reads export file and return a map of package->version.
     * Returned versions are in OSGi format but can be null if not specified in the file.
     *
     * @param exportFilePath the file path, never null.
     *
     * @return map of package->version, never null.
     */
    static Map<String, String> parseExportFile(String exportFilePath)
    {
        Properties props = new Properties();
        InputStream in = null;

        try
        {
            in = ClassLoaderUtils.getResourceAsStream(exportFilePath, ExportBuilderUtils.class);
            // this should automatically get rid of comment lines.
            props.load(in);
        }
        catch (IOException e)
        {
            LOG.warn("Problem occurred while processing package export", e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }

        // convert blank value to null and store in map so that only null means "unspecified version".
        Map<String, String> output = new HashMap<String, String>();
        for(Map.Entry entry:props.entrySet())
        {
            if (entry.getValue() != null && ((String)entry.getValue()).trim().length() > 0)
            {
                output.put((String)entry.getKey(), converter.getVersion((String)entry.getValue()));
            }
            else
            {
                output.put((String)entry.getKey(), null);
            }
        }

        return Collections.unmodifiableMap(output);
    }

    /**
     * Copies all the entries from src into dest for the keys that don't already exist in dest.
     */
    static void copyUnlessExist(Map<String, String> dest, Map<String, String> src)
    {
        for(Map.Entry<String, String> entry:src.entrySet())
        {
            if (!dest.containsKey(entry.getKey()))
            {
                dest.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Converts collection of ExportPackage into map of packageName->version.
     */
    static Map<String, String> toMap(Collection<ExportPackage> exportPackages)
    {
        Map<String, String> output = new HashMap<String, String>();
        for(ExportPackage pkg:exportPackages)
        {
            output.put(pkg.getPackageName(), pkg.getVersion());
        }
        return Collections.unmodifiableMap(output);
    }
}
