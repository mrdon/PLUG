package com.atlassian.plugin;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 */
public class PluginTestUtils
{
    public static final String SIMPLE_TEST_JAR;

    static
    {
        SIMPLE_TEST_JAR = "testjars/atlassian-plugins-simpletest-1.0.jar";
    }

    public static File getFileForResource(final String resourceName) throws URISyntaxException
    {
        return new File(new URI(PluginTestUtils.class.getClassLoader().getResource(resourceName).toString()));
    }
}
