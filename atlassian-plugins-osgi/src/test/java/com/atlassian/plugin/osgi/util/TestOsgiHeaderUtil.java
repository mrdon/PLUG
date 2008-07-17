package com.atlassian.plugin.osgi.util;

import junit.framework.TestCase;
import org.twdata.pkgscanner.ExportPackage;

import java.util.List;
import java.util.ArrayList;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.impl.MockRegistration;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;

import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableModel;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: 17/07/2008
 * Time: 2:11:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestOsgiHeaderUtil extends TestCase {

    public void testConstructAutoExports()
    {
        List<ExportPackage> exports = new ArrayList<ExportPackage>();
        exports.add(new ExportPackage("foo.bar", "1.0"));
        exports.add(new ExportPackage("foo.bar", "1.0-asdf-asdf"));
        assertEquals("foo.bar;version=1.0,foo.bar", OsgiHeaderUtil.constructAutoExports(exports));
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
}
