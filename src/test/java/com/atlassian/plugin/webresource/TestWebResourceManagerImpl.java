package com.atlassian.plugin.webresource;

import junit.framework.TestCase;
import com.atlassian.plugin.PluginAccessor;

import java.util.Map;
import java.util.HashMap;

/**
 * Tests the WebResourceManager
 * TODO: NEEDS MORE TESTS! Especially to assert that the correct HTML is being returned for the various resource
 * types.
 */
public class TestWebResourceManagerImpl extends TestCase
{

    private WebResourceManagerImpl manager;

    protected void setUp() throws Exception
    {
        manager = new WebResourceManagerImpl(new FakeWebResourceIntegration(new HashMap()));
    }

    public void testRequireResourceWithoutWriter()
    {
        //default should be delayed mode
        assertEquals(WebResourceManager.DELAYED_INCLUDE_MODE, manager.getIncludeMode());

        //lets add a resource and check
        manager.requireResource("resource1");

        // lets try the same in inline mode.  Should throw an exception.
        manager.setIncludeMode(WebResourceManager.INLINE_INCLUDE_MODE);
        assertEquals(WebResourceManager.INLINE_INCLUDE_MODE, manager.getIncludeMode());
        try
        {
            manager.requireResource("resource1");
            fail();
        } catch (IllegalStateException e)
        {
            //expected exception.
        }
    }

    public void testSettingIncludedMode()
    {
        //default should be delayed mode
        assertEquals(WebResourceManager.DELAYED_INCLUDE_MODE, manager.getIncludeMode());

        // lets try the same in inline mode. 
        manager.setIncludeMode(WebResourceManager.INLINE_INCLUDE_MODE);
        assertEquals(WebResourceManager.INLINE_INCLUDE_MODE, manager.getIncludeMode());

        // lets switch back
        manager.setIncludeMode(WebResourceManager.DELAYED_INCLUDE_MODE);
        assertEquals(WebResourceManager.DELAYED_INCLUDE_MODE, manager.getIncludeMode());
    }

    private class FakeWebResourceIntegration implements WebResourceIntegration
    {
        private final Map requestCache;


        public FakeWebResourceIntegration(Map requestCache)
        {
            this.requestCache = requestCache;
        }

        public PluginAccessor getPluginAccessor()
        {
            return null;
        }

        public Map getRequestCache()
        {
            return requestCache;
        }

        public String getSystemCounter()
        {
            return null;
        }

        public String getSystemBuildNumber()
        {
            return null;
        }

        public String getBaseUrl()
        {
            return null;
        }
    }
}
