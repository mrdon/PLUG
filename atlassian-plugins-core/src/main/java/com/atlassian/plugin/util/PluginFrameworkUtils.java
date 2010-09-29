package com.atlassian.plugin.util;

import com.atlassian.util.concurrent.LazyReference;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * General utility functions for plugin framework.
 *
 * @since 2.7.0
 */
public final class PluginFrameworkUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(PluginFrameworkUtils.class);
    private static final String BUILD_PROPERTY_PATH = "META-INF/maven/com.atlassian.plugins/atlassian-plugins-core/pom.properties";

    /**
     * Not for instantiation.
     */
    private PluginFrameworkUtils()
    {}

    /**
     * Get the current plugin framework version.
     * This is not necessarily in OSGi format as it is generated by maven during build process.
     *
     * @return current plugin framework version.
     */
    public static String getPluginFrameworkVersion()
    {
        return pluginFrameworkVersionRef.get();
    }

    private static final LazyReference<String> pluginFrameworkVersionRef = new LazyReference<String>()
    {
        @Override
        protected String create() throws Exception
        {
            return getPluginFrameworkVersionInternal();
        }
    };

    private static String getPluginFrameworkVersionInternal()
    {
        Properties props = new Properties();
        InputStream in = null;

        try
        {
            in = ClassLoaderUtils.getResourceAsStream(BUILD_PROPERTY_PATH, PluginFrameworkUtils.class);
            // this should automatically get rid of comment lines.
            props.load(in);
            return props.getProperty("version");
        }
        catch (IOException e)
        {
            LOG.error("cannot determine the plugin framework version", e);
            throw new IllegalStateException("cannot determine the plugin framework version", e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }
}
