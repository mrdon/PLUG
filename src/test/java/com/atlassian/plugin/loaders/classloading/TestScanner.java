package com.atlassian.plugin.loaders.classloading;

import junit.framework.TestCase;

import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.File;
import java.util.*;

import com.atlassian.plugin.loaders.TestClassPathPluginLoader;
import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.plugin.util.FileUtils;

public class TestScanner extends TestCase
{
    public void testNormalOperation() throws Exception
    {
        File pluginsDirectory = getPluginsDirectory();
        Scanner scanner = new Scanner(pluginsDirectory);
        scanner.scan();
        Collection deployedUnits = scanner.getDeploymentUnits();
        assertEquals(2, deployedUnits.size());

        // put them into a list so we're sure we get them in the right order
        Set orderedUnits = new TreeSet(deployedUnits);
        Iterator iterator = orderedUnits.iterator();
        DeploymentUnit unit = (DeploymentUnit) iterator.next();
        assertEquals("paddington-test-plugin.jar", unit.getPath().getName());

        unit = (DeploymentUnit) iterator.next();
        assertEquals("pooh-test-plugin.jar", unit.getPath().getName());
    }

    public void testAddAndRemoveJar() throws Exception
    {
        File pluginsDirectory = getPluginsDirectory();
        File paddington = new File(pluginsDirectory, "paddington-test-plugin.jar");
        File duplicate = new File(pluginsDirectory, "duplicate-test-plugin.jar");

        // make sure it's not thate from a bad test
        if (duplicate.canRead())
            duplicate.delete();

        // should be 2 to start with
        Scanner scanner = new Scanner(pluginsDirectory);
        scanner.scan();
        assertEquals(2, scanner.getDeploymentUnits().size());

        // copy and scan - should have 3 files
        FileUtils.copyFile(paddington, duplicate);
        scanner.scan();
        assertEquals(3, scanner.getDeploymentUnits().size());

        // delete and we should have 2!
        duplicate.delete();
        scanner.scan();
        assertEquals(2, scanner.getDeploymentUnits().size());
    }


    public void testModifyJar() throws Exception
    {
        File pluginsDirectory = getPluginsDirectory();
        File paddington = new File(pluginsDirectory, "paddington-test-plugin.jar");

        // set the original mod date - must be a multiple of 1000 on most (all?) file systems
        long originalModification = System.currentTimeMillis();
        originalModification = originalModification - (originalModification % 1000);
        paddington.setLastModified(originalModification);

        // should be 2 to start with
        Scanner scanner = new Scanner(pluginsDirectory);
        scanner.scan();
        assertEquals(2, scanner.getDeploymentUnits().size());

        DeploymentUnit paddingtonUnit = scanner.locateDeploymentUnit(paddington);
        assertEquals(originalModification, paddingtonUnit.lastModified());

        // copy and scan - should have 3 files
        long secondModification = originalModification + 2000;
        paddington.setLastModified(secondModification);
        scanner.scan();

        paddingtonUnit = scanner.locateDeploymentUnit(paddington);
        assertEquals(secondModification, paddingtonUnit.lastModified());
    }

    public void testAcceptOnlyJar() throws Exception
    {
        File pluginsDirectory = getPluginsDirectory();
        Scanner scanner = new Scanner(pluginsDirectory);
        assertTrue(scanner.accept(new File("myfile.jar")));
        assertFalse(scanner.accept(new File("myfile.txt")));
    }

    private File getPluginsDirectory()
            throws URISyntaxException
    {
        // hacky way of getting to the directoryPluginLoaderFiles classloading
        URL url = ClassLoaderUtils.getResource("test-disabled-plugin.xml", TestClassPathPluginLoader.class);
        File disabledPluginXml = new File(new URI(url.toExternalForm()));
        File directoryPluginLoaderFiles = new File(disabledPluginXml.getParentFile().getParentFile(), "classLoadingTestFiles");
        File pluginsDirectory = new File(directoryPluginLoaderFiles, "plugins");
        return pluginsDirectory;
    }
}
