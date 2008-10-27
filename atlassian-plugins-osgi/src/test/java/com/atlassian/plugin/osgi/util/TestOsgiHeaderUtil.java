package com.atlassian.plugin.osgi.util;

import com.atlassian.plugin.osgi.factory.transform.StubHostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;

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
