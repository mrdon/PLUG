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
}
