package com.atlassian.plugin.osgi.util;

import com.atlassian.plugin.osgi.factory.transform.StubHostComponentRegistration;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.impl.MockRegistration;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import junit.framework.TestCase;

import java.io.IOException;
import javax.servlet.ServletContext;
import javax.print.attribute.HashAttributeSet;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AttributeSet;
import java.util.*;

import org.twdata.pkgscanner.ExportPackage;

public class TestOsgiHeaderUtil extends TestCase 
{

    public void testFindReferredPackages() throws IOException
    {
        String foundPackages = OsgiHeaderUtil.findReferredPackages(new ArrayList<HostComponentRegistration>()
        {{
            add(new StubHostComponentRegistration(OsgiHeaderUtil.class));
        }});

        assertTrue(foundPackages.contains(HostComponentRegistration.class.getPackage().getName()));

    }
}
