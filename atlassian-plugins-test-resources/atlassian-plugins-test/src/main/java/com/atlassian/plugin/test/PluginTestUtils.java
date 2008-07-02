package com.atlassian.plugin.test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URI;

/**
 *
 */
public class PluginTestUtils
{
    public static final String PROJECT_VERSION;
    public static final String SIMPLE_TEST_JAR;
    public static final String INNER1_TEST_JAR;
    public static final String INNER2_TEST_JAR;

    static
    {
        PROJECT_VERSION = System.getProperty("project.version");
        SIMPLE_TEST_JAR = "atlassian-plugins-simpletest-" + PROJECT_VERSION + ".jar";
        INNER1_TEST_JAR = "atlassian-plugins-innerjarone-" + PROJECT_VERSION + ".jar";
        INNER2_TEST_JAR = "atlassian-plugins-innerjartwo-" + PROJECT_VERSION + ".jar";
    }

    public static File getFileForResource(final String resourceName) throws URISyntaxException
    {
        return new File(new URI(PluginTestUtils.class.getClassLoader().getResource(resourceName).toString()));
    }
}
