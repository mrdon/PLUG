package com.atlassian.plugin.loaders;

import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * 
 */
public class TestFileListScanner extends TestCase
{
    public void testNormalOperation() throws Exception
    {
        FileListScanner scanner = new FileListScanner(Arrays.asList(new File("foo.txt"), new File("bar.txt")));
        assertContainsFooAndBar(scanner);

        assertEquals(0, scanner.scan().size()); // second time, no new units

        scanner.reset();
        assertContainsFooAndBar(scanner);
    }

    private void assertContainsFooAndBar(final FileListScanner scanner)
    {
        Collection<DeploymentUnit> scan = scanner.scan();
        assertEquals(2, scan.size());
        final Iterator<DeploymentUnit> i = scan.iterator();
        i.hasNext();
        assertEquals("foo.txt", i.next().getPath().getName());
        i.hasNext();
        assertEquals("bar.txt", i.next().getPath().getName());
    }

}
