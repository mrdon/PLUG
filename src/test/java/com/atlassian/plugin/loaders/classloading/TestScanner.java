package com.atlassian.plugin.loaders.classloading;

import com.atlassian.plugin.util.FileUtils;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class TestScanner extends AbstractTestClassLoader
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

        // make sure it's not there already from a bad test
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
        // Note windows is a piece of shit (can't modify files in the classpath) so
        // we need to create a temporary directory outside the classpath where we can modify files freely.
        File pluginsDirectory = getPluginsDirectory();
        File paddington = new File(pluginsDirectory, "paddington-test-plugin.jar");

        File testTempDirectory = new File(System.getProperty("java.io.tmpdir") + File.separator + "TestScannerTests");

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
        Scanner scanner = new Scanner(testTempDirectory);
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

    public void testAcceptOnlyJar() throws Exception
    {
        Scanner.JarFileFilter filter = new Scanner.JarFileFilter();
        assertTrue(filter.accept(new File("myfile.jar")));
        assertFalse(filter.accept(new File("myfile.txt")));
    }
}
