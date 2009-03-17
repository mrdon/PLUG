package com.atlassian.plugin.loaders;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import com.atlassian.plugin.loaders.classloading.AbstractTestClassLoader;
import com.atlassian.plugin.loaders.classloading.Scanner;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

public class TestDirectoryScanner extends AbstractTestClassLoader
{
    protected void setUp() throws Exception
    {
        super.setUp();

        super.createFillAndCleanTempPluginDirectory();
    }

    public void testNormalOperation() throws Exception
    {
        File pluginsDirectory = pluginsTestDir;
        Scanner scanner = new DirectoryScanner(pluginsDirectory);
        scanner.scan();
        Collection<DeploymentUnit> deployedUnits = scanner.getDeploymentUnits();
        assertEquals(2, deployedUnits.size());

        // units should be returned ordered alphabetically
        Iterator iterator = deployedUnits.iterator();
        DeploymentUnit unit = (DeploymentUnit) iterator.next();
        assertEquals("paddington-test-plugin.jar", unit.getPath().getName());

        unit = (DeploymentUnit) iterator.next();
        assertEquals("pooh-test-plugin.jar", unit.getPath().getName());
    }

    public void testSkipDot() throws Exception
    {
        File pluginsDirectory = pluginsTestDir;
        assertNotNull(File.createTempFile(".asdf", ".jar", pluginsDirectory));
        Scanner scanner = new DirectoryScanner(pluginsDirectory);
        scanner.scan();
        Collection<DeploymentUnit> deployedUnits = scanner.getDeploymentUnits();
        assertEquals(2, deployedUnits.size());

        // units should be returned ordered alphabetically
        Iterator iterator = deployedUnits.iterator();
        DeploymentUnit unit = (DeploymentUnit) iterator.next();
        assertEquals("paddington-test-plugin.jar", unit.getPath().getName());

        unit = (DeploymentUnit) iterator.next();
        assertEquals("pooh-test-plugin.jar", unit.getPath().getName());
    }

    public void testRemove()
    {
        File pluginsDirectory = pluginsTestDir;
        File paddington = new File(pluginsDirectory, "paddington-test-plugin.jar");

        assertTrue(paddington.exists());

        DirectoryScanner scanner = new DirectoryScanner(pluginsDirectory);
        scanner.scan();
        assertEquals(2, scanner.getDeploymentUnits().size());
        DeploymentUnit paddingtonUnit = scanner.locateDeploymentUnit(paddington);
        scanner.remove(paddingtonUnit);

        assertFalse(paddington.exists());
        assertEquals(1, scanner.getDeploymentUnits().size());
    }

    public void testAddAndRemoveJarFromOutsideScanner() throws Exception
    {
        File pluginsDirectory = pluginsTestDir;
        File paddington = new File(pluginsDirectory, "paddington-test-plugin.jar");
        File duplicate = new File(pluginsDirectory, "duplicate-test-plugin.jar");

        // make sure it's not there already from a bad test
        if (duplicate.canRead())
            duplicate.delete();

        // should be 2 to start with
        Scanner scanner = new DirectoryScanner(pluginsDirectory);
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
        // Note windows is a piece of shit (can't modify files in the classpath) so
        // we need to create a temporary directory outside the classpath where we can modify files freely.
        File pluginsDirectory = pluginsTestDir;
        File paddington = new File(pluginsDirectory, "paddington-test-plugin.jar");

        File testTempDirectory = new File("target/plugins-temp/TestScannerTests");

        if (testTempDirectory.exists()) // if the directory exists, nuke it.
            testTempDirectory.delete();

        testTempDirectory.mkdirs();

        File newPaddington = new File(testTempDirectory, "paddington-test-plugin.jar");
        FileUtils.copyFile(paddington, newPaddington);

        // set the original mod date - must be a multiple of 1000 on most (all?) file systems
        long originalModification = System.currentTimeMillis();
        originalModification = originalModification - (originalModification % 1000) - 3000;
        newPaddington.setLastModified(originalModification);
        assertEquals(originalModification, newPaddington.lastModified());

        // should be 2 to start with
        DirectoryScanner scanner = new DirectoryScanner(testTempDirectory);
        scanner.scan();
        assertEquals(1, scanner.getDeploymentUnits().size());

        DeploymentUnit newPaddingtonUnit = scanner.locateDeploymentUnit(newPaddington);
        assertEquals(originalModification, newPaddingtonUnit.lastModified());

        // modify the JAR file
        long secondModification = originalModification + 2000;
        newPaddington.setLastModified(secondModification);
        scanner.scan();

        newPaddingtonUnit = scanner.locateDeploymentUnit(newPaddington);
        assertEquals(secondModification, newPaddingtonUnit.lastModified());
    }

}
