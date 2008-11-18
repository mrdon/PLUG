package com.atlassian.plugin.osgi.util;

import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.impl.MockRegistration;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import junit.framework.TestCase;
import org.twdata.pkgscanner.ExportPackage;

import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.servlet.ServletContext;
import java.util.*;

public class TestOsgiHeaderUtil extends TestCase {

    public void testDetermineExports()
    {
        String exports = OsgiHeaderUtil.determineExports(new ArrayList<HostComponentRegistration>(), new DefaultPackageScannerConfiguration());
        assertFalse(exports.contains(",,"));
    }

    public void testConstructAutoExports()
    {
        List<ExportPackage> exports = new ArrayList<ExportPackage>();
        exports.add(new ExportPackage("foo.bar", "1.0"));
        exports.add(new ExportPackage("foo.bar", "1.0-asdf-asdf"));
        StringBuilder sb = new StringBuilder();
        OsgiHeaderUtil.constructAutoExports(sb, exports);

        assertEquals("foo.bar;version=1.0,foo.bar,", sb.toString());
    }

    public void testDetermineExportsIncludeServiceInterfaces()
    {
        List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration> () {{
            add(new MockRegistration(new HashAttributeSet(), AttributeSet.class));
            add(new MockRegistration(new DefaultTableModel(), TableModel.class));
        }};
        String imports = OsgiHeaderUtil.determineExports(regs, new DefaultPackageScannerConfiguration());
        assertNotNull(imports);
        System.out.println(imports.replace(',','\n'));
        assertTrue(imports.contains(AttributeSet.class.getPackage().getName()));
        assertTrue(imports.contains("javax.swing.event"));
    }

    public void testGenerateExports()
    {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getResource", C.args(C.eq("/WEB-INF/lib")), getClass().getClassLoader().getResource("scanbase/WEB-INF/lib"));
        mockServletContext.expectAndReturn("getResource", C.args(C.eq("/WEB-INF/classes")), getClass().getClassLoader().getResource("scanbase/WEB-INF/classes"));
        DefaultPackageScannerConfiguration config = new DefaultPackageScannerConfiguration();
        config.setPackageIncludes(Arrays.asList("org.*"));
        config.setServletContext((ServletContext) mockServletContext.proxy());

        Collection<ExportPackage> exports = OsgiHeaderUtil.generateExports(config);
        assertNotNull(exports);
        assertTrue(exports.contains(new ExportPackage("org.apache.log4j", "1.2.15")));

        // Test falling through to servlet context scanning
        config.setJarIncludes(Arrays.asList("testlog*"));
        config.setJarExcludes(Arrays.asList("log4j*"));
        exports = OsgiHeaderUtil.generateExports(config);
        assertNotNull(exports);
        assertTrue(exports.contains(new ExportPackage("org.apache.log4j", "1.2.15")));

        // Test failure when even servlet context scanning fails
        mockServletContext.expectAndReturn("getResource", C.args(C.eq("/WEB-INF/lib")), getClass().getClassLoader().getResource("scanbase/WEB-INF/lib"));
        mockServletContext.expectAndReturn("getResource", C.args(C.eq("/WEB-INF/classes")), getClass().getClassLoader().getResource("scanbase/WEB-INF/classes"));
        config.setJarIncludes(Arrays.asList("testlog4j23*"));
        config.setJarExcludes(Collections.<String>emptyList());
        try
        {
            exports = OsgiHeaderUtil.generateExports(config);
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
            exports = OsgiHeaderUtil.generateExports(config);
            fail("Should have thrown an exception");
        } catch (IllegalStateException ex)
        {
            // good stuff
        }

        mockServletContext.verify();
    }

    public void testConstructJdkExports()
    {
        StringBuilder sb = new StringBuilder();
        OsgiHeaderUtil.constructJdkExports(sb,"jdk-packages.test.txt");
        assertEquals("foo.bar,foo.baz", sb.toString());
        sb = new StringBuilder();
        OsgiHeaderUtil.constructJdkExports(sb, OsgiHeaderUtil.JDK_PACKAGES_PATH);
        assertTrue(sb.toString().contains("org.xml.sax"));
    }

    public void testConstructJdkExportsWithJdk5And6()
    {
        String jdkVersion = System.getProperty("java.specification.version");
        try
        {
            System.setProperty("java.specification.version", "1.5");
            String exports = OsgiHeaderUtil.determineExports(new ArrayList<HostComponentRegistration>(), new DefaultPackageScannerConfiguration());
            assertFalse(exports.contains("javax.script"));
            System.setProperty("java.specification.version", "1.6");
            exports = OsgiHeaderUtil.determineExports(new ArrayList<HostComponentRegistration>(), new DefaultPackageScannerConfiguration());
            assertTrue(exports.contains("javax.script"));
        }
        finally
        {
            System.setProperty("java.specification.version", jdkVersion);
        }


    }


}
