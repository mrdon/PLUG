package com.atlassian.plugin.loaders.classloading;

import com.atlassian.plugin.util.ClassLoaderUtils;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

public abstract class AbstractTestClassLoader extends TestCase
{
    public static final String PADDINGTON_JAR = "paddington-test-plugin.jar";
    public static final String POOH_JAR = "pooh-test-plugin.jar";

    protected File pluginsDirectory;
    protected File tempDir;
    protected File pluginsTestDir;

    protected File getPluginsDirectory()
    {
        final URL url = ClassLoaderUtils.getResource("ap-plugins", this.getClass());
        pluginsDirectory = new File(url.getFile());
        return pluginsDirectory;
    }

    /**
     * Generate a random string of characters - including numbers
     *
     * @param length the length of the string to return
     * @return a random string of characters
     */
    public static String randomString(int length)
    {
        StringBuffer b = new StringBuffer(length);

        for (int i = 0; i < length; i++)
        {
            b.append(randomAlpha());
        }

        return b.toString();
    }

    /**
     * Generate a random character from the alphabet - either a-z or A-Z
     *
     * @return a random alphabetic character
     */
    public static char randomAlpha()
    {
        int i = (int) (Math.random() * 52);

        if (i > 25)
            return (char) (97 + i - 26);
        else
            return (char) (65 + i);
    }

    protected void createFillAndCleanTempPluginDirectory() throws IOException
    {
        pluginsDirectory = getPluginsDirectory(); // hacky way of getting to the directoryPluginLoaderFiles classloading
        tempDir = new File("target/plugins-temp");

        File pluginsDir = new File(tempDir.toString() + File.separator +  "plugins");
        pluginsTestDir = new File(pluginsDir, randomString(6));

        if (pluginsDir.exists() && pluginsDir.isDirectory())
            FileUtils.deleteDirectory(pluginsDir);

        pluginsTestDir.mkdirs();

        FileUtils.copyDirectory(pluginsDirectory, pluginsTestDir);

        // Clean up CVS directory in case we copied it over by mistake.
        removeSourceControlMetadata("CVS");

        // Clean up SVN directory in case we copied it over by mistake.
        removeSourceControlMetadata(".svn");
    }

    private void removeSourceControlMetadata(String directoryName) throws IOException
    {
        File dir = new File(pluginsTestDir, directoryName);
        if (dir.exists()) FileUtils.deleteDirectory(dir);
    }
}
