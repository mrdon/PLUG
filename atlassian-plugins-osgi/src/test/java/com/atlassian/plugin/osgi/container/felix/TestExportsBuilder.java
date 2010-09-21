package com.atlassian.plugin.osgi.container.felix;

import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.impl.MockRegistration;
import junit.framework.TestCase;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.twdata.pkgscanner.ExportPackage;

import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.servlet.ServletContext;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestExportsBuilder extends TestCase
{
    private ExportsBuilder builder;

    @Override
    public void setUp() throws Exception
    {
        builder = new ExportsBuilder();
    }

    @Override
    public void tearDown() throws Exception
    {
        builder = null;
    }
    public void testDetermineExports()
    {
        DefaultPackageScannerConfiguration config = new DefaultPackageScannerConfiguration("0.0");

        String exports = builder.determineExports(new ArrayList<HostComponentRegistration>(), config);
        assertFalse(exports.contains(",,"));
    }

    public void testDetermineExportsIncludeServiceInterfaces()
    {
        List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration> () {{
            add(new MockRegistration(new HashAttributeSet(), AttributeSet.class));
            add(new MockRegistration(new DefaultTableModel(), TableModel.class));
        }};
        String imports = builder.determineExports(regs, new DefaultPackageScannerConfiguration());
        assertNotNull(imports);
        System.out.println(imports.replace(',','\n'));
        assertTrue(imports.contains(AttributeSet.class.getPackage().getName()));
        assertTrue(imports.contains("javax.swing.event"));
    }

    public void testConstructJdkExportsWithJdk5And6()
    {
        String jdkVersion = System.getProperty("java.specification.version");
        try
        {
            System.setProperty("java.specification.version", "1.5");
            String exports = builder.determineExports(new ArrayList<HostComponentRegistration>(), new DefaultPackageScannerConfiguration());
            assertFalse(exports.contains("javax.script"));
            System.setProperty("java.specification.version", "1.6");
            exports = builder.determineExports(new ArrayList<HostComponentRegistration>(), new DefaultPackageScannerConfiguration());
            assertTrue(exports.contains("javax.script"));
        }
        finally
        {
            System.setProperty("java.specification.version", jdkVersion);
        }
    }


    public void testDetermineExportWhileConflictExists()
    {
        final DescriptorAccess descAccess = new DescriptorAccess() {
            public Descriptor getDescriptor()
            {
                return null;
            }
            public void setDescriptor(Descriptor inDescriptor)
            {
            }
        };

        List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration> () {{
            add(new MockRegistration(descAccess, DescriptorAccess.class));
        }};

        DefaultPackageScannerConfiguration config = new DefaultPackageScannerConfiguration();
        Map<String, String> versions = new HashMap<String, String>();
        versions.put("javax.management", "1.2.3");
        config.setPackageVersions(versions);

        String exports = builder.determineExports(regs, config);

        int packageCount = 0;
        for(String imp:exports.split("[,]"))
        {
            if (imp.split("[;]")[0].equals("javax.management"))
            {
                packageCount++;
            }
        }

        assertEquals("even though the package is found twice, we must export it only once", 1, packageCount);
        assertFalse("found earlier always wins", exports.contains("javax.management;version=1.2.3"));
        assertTrue("found earlier always wins", exports.contains(",javax.management,"));
    }

    public void testGenerateExports() throws MalformedURLException
    {
        ServletContext ctx = mock(ServletContext.class);
        when(ctx.getMajorVersion()).thenReturn(5);
        when(ctx.getMinorVersion()).thenReturn(3);
        when(ctx.getResource("/WEB-INF/lib")).thenReturn(getClass().getClassLoader().getResource("scanbase/WEB-INF/lib"));
        when(ctx.getResource("/WEB-INF/classes")).thenReturn(getClass().getClassLoader().getResource("scanbase/WEB-INF/classes"));
        DefaultPackageScannerConfiguration config = new DefaultPackageScannerConfiguration("1.0");
        config.setServletContext(ctx);
        config.setPackageIncludes(Arrays.asList("javax.*", "org.*"));

        Collection<ExportPackage> exports = builder.generateExports(config);
        assertNotNull(exports);
        assertTrue(exports.contains(new ExportPackage("org.apache.log4j", "1.2.15", new File("/whatever/log4j-1.2.15.jar"))));

        // Test falling through to servlet context scanning
        config.setJarIncludes(Arrays.asList("testlog*", "mock*"));
        config.setJarExcludes(Arrays.asList("log4j*"));
        exports = builder.generateExports(config);
        assertNotNull(exports);
        assertTrue(exports.contains(new ExportPackage("org.apache.log4j", "1.2.15", new File("/whatever/log4j-1.2.15.jar"))));

        // Test failure when even servlet context scanning fails
        config.setJarIncludes(Arrays.asList("testlog4j23*"));
        config.setJarExcludes(Collections.<String>emptyList());
        try
        {
            exports = builder.generateExports(config);
            fail("Should have thrown an exception");
        }
        catch (IllegalStateException ex)
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
        }
        catch (IllegalStateException ex)
        {
            // good stuff
        }
    }

    public void testGenerateExportsWithCorrectServletVersion() throws MalformedURLException
    {
        ServletContext ctx = mock(ServletContext.class);
        when(ctx.getMajorVersion()).thenReturn(5);
        when(ctx.getMinorVersion()).thenReturn(3);
        when(ctx.getResource("/WEB-INF/lib")).thenReturn(getClass().getClassLoader().getResource("scanbase/WEB-INF/lib"));
        when(ctx.getResource("/WEB-INF/classes")).thenReturn(getClass().getClassLoader().getResource("scanbase/WEB-INF/classes"));

        DefaultPackageScannerConfiguration config = new DefaultPackageScannerConfiguration("1.0");
        config.setServletContext(ctx);
        config.setPackageIncludes(Arrays.asList("javax.*", "org.*"));

        Collection<ExportPackage> exports = builder.generateExports(config);

        int pkgsToFind = 2;
        for (ExportPackage pkg : exports)
        {
            if ("javax.servlet".equals(pkg.getPackageName())) {
                assertEquals("5.3.0", pkg.getVersion());
                pkgsToFind--;
            }
            if ("javax.servlet.http".equals(pkg.getPackageName())) {
                assertEquals("5.3.0", pkg.getVersion());
                pkgsToFind--;
            }
        }
        assertEquals(0, pkgsToFind);
    }




}
