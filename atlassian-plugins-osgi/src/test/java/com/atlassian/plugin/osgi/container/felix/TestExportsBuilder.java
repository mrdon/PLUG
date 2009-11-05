package com.atlassian.plugin.osgi.container.felix;

import junit.framework.TestCase;

import java.io.File;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.twdata.pkgscanner.ExportPackage;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.impl.MockRegistration;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;

import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.AttributeSet;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.servlet.ServletContext;

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

    public void testConstructAutoExports()
    {
        List<ExportPackage> exports = new ArrayList<ExportPackage>();
        exports.add(new ExportPackage("foo.bar", "1.0", new File("/whatever/foobar-1.0.jar")));
        exports.add(new ExportPackage("foo.bar", "1.0-asdf-asdf", new File("/whatever/foobar-1.0-asdf-asdf.jar")));
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

    public void testConstructJdkExportsWithJdk5And6()
    {
        String jdkVersion = System.getProperty("java.specification.version");
        try
        {
            System.setProperty("java.specification.version", "1.5");
            String exports = builder.determineExports(new ArrayList<HostComponentRegistration>(), new DefaultPackageScannerConfiguration(), tmpDir);
            assertFalse(exports.contains("javax.script"));
            System.setProperty("java.specification.version", "1.6");
            exports = builder.determineExports(new ArrayList<HostComponentRegistration>(), new DefaultPackageScannerConfiguration(), tmpDir);
            assertTrue(exports.contains("javax.script"));
        }
        finally
        {
            System.setProperty("java.specification.version", jdkVersion);
        }
    }


    public void testGenerateExports()
        {
            Mock mockServletContext = new Mock(ServletContext.class);
            mockServletContext.expectAndReturn("getResource", C.args(C.eq("/WEB-INF/lib")), getClass().getClassLoader().getResource("scanbase/WEB-INF/lib"));
            mockServletContext.expectAndReturn("getResource", C.args(C.eq("/WEB-INF/classes")), getClass().getClassLoader().getResource("scanbase/WEB-INF/classes"));
            DefaultPackageScannerConfiguration config = new DefaultPackageScannerConfiguration();
            config.setPackageIncludes(Arrays.asList("org.*"));
            config.setServletContext((ServletContext) mockServletContext.proxy());

            Collection<ExportPackage> exports = builder.generateExports(config);
            assertNotNull(exports);
            assertTrue(exports.contains(new ExportPackage("org.apache.log4j", "1.2.15", new File("/whatever/log4j-1.2.15.jar"))));

            // Test falling through to servlet context scanning
            config.setJarIncludes(Arrays.asList("testlog*"));
            config.setJarExcludes(Arrays.asList("log4j*"));
            exports = builder.generateExports(config);
            assertNotNull(exports);
            assertTrue(exports.contains(new ExportPackage("org.apache.log4j", "1.2.15", new File("/whatever/log4j-1.2.15.jar"))));

            // Test failure when even servlet context scanning fails
            mockServletContext.expectAndReturn("getResource", C.args(C.eq("/WEB-INF/lib")), getClass().getClassLoader().getResource("scanbase/WEB-INF/lib"));
            mockServletContext.expectAndReturn("getResource", C.args(C.eq("/WEB-INF/classes")), getClass().getClassLoader().getResource("scanbase/WEB-INF/classes"));
            config.setJarIncludes(Arrays.asList("testlog4j23*"));
            config.setJarExcludes(Collections.<String>emptyList());
            try
            {
                exports = builder.generateExports(config);
                fail("Should have thrown an exception");
            } catch (IllegalStateException ex)
            {
                // good stuff
            }

            // Test failure when no servlet context
            config.setJarIncludes(Arrays.asList("testlog4j23*"));
            config.setJarExcludes(Collections.<String>emptyList());
            config.setServletContext(null);
            try
            {
                exports = builder.generateExports(config);
                fail("Should have thrown an exception");
            } catch (IllegalStateException ex)
            {
                // good stuff
            }

            mockServletContext.verify();
        }

}
