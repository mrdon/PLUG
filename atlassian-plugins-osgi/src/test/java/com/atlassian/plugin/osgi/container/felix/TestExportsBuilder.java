package com.atlassian.plugin.osgi.container.felix;

import junit.framework.TestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.twdata.pkgscanner.ExportPackage;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.impl.MockRegistration;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;

import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.AttributeSet;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class TestExportsBuilder extends TestCase
{
    private File tmpDir;
    private ExportsBuilder builder;

    @Override
    public void setUp() throws Exception
    {
        tmpDir = new File("target/temp");
        if (tmpDir.exists())  FileUtils.cleanDirectory(tmpDir);
        tmpDir.mkdirs();
        builder = new ExportsBuilder();
    }

    @Override
    public void tearDown() throws Exception
    {
        tmpDir = null;
        builder = null;
    }
    public void testDetermineExports()
    {
        DefaultPackageScannerConfiguration config = new DefaultPackageScannerConfiguration("0.0");

        String exports = builder.determineExports(new ArrayList<HostComponentRegistration>(), config, tmpDir);
        assertFalse(exports.contains(",,"));
    }

    public void testDetermineExportsUseCache()
    {
        final String myPackage = ExportsBuilder.class.getPackage().getName();

        DefaultPackageScannerConfiguration config = new DefaultPackageScannerConfiguration("0.0");

        String exports = builder.determineExports(new ArrayList<HostComponentRegistration>(), config, tmpDir);
        assertFalse(exports.contains(",,"));
        assertTrue(new File(tmpDir, "exports.txt").exists());
        assertTrue(exports.contains(myPackage));
        config.setPackageExcludes(Arrays.asList(myPackage));

        String exports2 = builder.determineExports(new ArrayList<HostComponentRegistration>(), config, tmpDir);
        assertTrue(exports2.contains(myPackage));

        config = new DefaultPackageScannerConfiguration("1.0");
        config.setPackageExcludes(Arrays.asList(myPackage));

        String exports3 = builder.determineExports(new ArrayList<HostComponentRegistration>(), config, tmpDir);
        assertFalse(exports3.contains(myPackage));
    }

    public void testConstructAutoExports()
    {
        List<ExportPackage> exports = new ArrayList<ExportPackage>();
        exports.add(new ExportPackage("foo.bar", "1.0"));
        exports.add(new ExportPackage("foo.bar", "1.0-asdf-asdf"));
        StringBuilder sb = new StringBuilder();
        builder.constructAutoExports(sb, exports);

        assertEquals("foo.bar;version=1.0,foo.bar,", sb.toString());
    }

    public void testDetermineExportsIncludeServiceInterfaces()
    {
        List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration> () {{
            add(new MockRegistration(new HashAttributeSet(), AttributeSet.class));
            add(new MockRegistration(new DefaultTableModel(), TableModel.class));
        }};
        String imports = builder.determineExports(regs, new DefaultPackageScannerConfiguration(), tmpDir);
        assertNotNull(imports);
        System.out.println(imports.replace(',','\n'));
        assertTrue(imports.contains(AttributeSet.class.getPackage().getName()));
        assertTrue(imports.contains("javax.swing.event"));
    }

    public void testConstructJdkExports()
    {
        StringBuilder sb = new StringBuilder();
        builder.constructJdkExports(sb,"jdk-packages.test.txt");
        assertEquals("foo.bar,foo.baz", sb.toString());
        sb = new StringBuilder();
        builder.constructJdkExports(sb, ExportsBuilder.JDK_PACKAGES_PATH);
        assertTrue(sb.toString().contains("org.xml.sax"));
    }
}
