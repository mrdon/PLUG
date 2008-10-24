package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.servlet.AbstractFileServerServlet;
import com.atlassian.plugin.webresource.mock.StubModuleDescriptor;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

public class TestWebResourceManagerImpl extends TestCase
{
    private WebResourceManagerImpl webResourceManager;

    private static final String ANIMAL_PLUGIN_VERSION = "2.1";
    private static final String BASEURL = "http://www.foo.com";
    private static final String SYSTEM_COUNTER = "123";
    private static final String SYSTEM_BUILD_NUMBER = "650";

    protected void setUp() throws Exception
    {
        super.setUp();

        Mock webResourceIntegration = new Mock(WebResourceIntegration.class);
        webResourceManager = new WebResourceManagerImpl((WebResourceIntegration) webResourceIntegration.proxy());

        webResourceIntegration.expectAndReturn("getBaseUrl", BASEURL);
        webResourceIntegration.expectAndReturn("getSystemBuildNumber", SYSTEM_BUILD_NUMBER);
        webResourceIntegration.expectAndReturn("getSystemCounter", SYSTEM_COUNTER);
    }

    protected void tearDown() throws Exception
    {
        webResourceManager = null;

        super.tearDown();
    }

    public void testGetStaticResourcePrefix()
    {
        String expectedPrefix = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix());
    }

    public void testGetStaticResourcePrefixWithCounter()
    {
        String resourceCounter = "456";
        String expectedPrefix = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + resourceCounter + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix(resourceCounter));
    }

    public void testGetStaticPluginResourcePrefix() throws UnsupportedEncodingException
    {
        Plugin animalPlugin = new StaticPlugin();
        PluginInformation pluginInfo = new PluginInformation();
        pluginInfo.setVersion(ANIMAL_PLUGIN_VERSION);
        animalPlugin.setKey("confluence.extra.animal:animal");
        animalPlugin.setPluginInformation(pluginInfo);

        final  StubModuleDescriptor animalModuleDescriptor = new StubModuleDescriptor();
        animalModuleDescriptor.setPlugin(animalPlugin);
        String encodedDescriptorKey = URLEncoder.encode(animalModuleDescriptor.getCompleteKey(), "UTF-8");

        String resourceName = "foo.js";
        String expectedPrefix = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + ANIMAL_PLUGIN_VERSION + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX + "/" + AbstractFileServerServlet
            .SERVLET_PATH + "/" + AbstractFileServerServlet.RESOURCE_URL_PREFIX + "/" + encodedDescriptorKey + "/" + resourceName;
        assertEquals(expectedPrefix, webResourceManager.getStaticPluginResource(animalModuleDescriptor, resourceName));
    }

    public void testGetResourceUrl()
    {
        Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expectAndReturn("getCompleteKey", "foo:bar");
        String url = webResourceManager.getResourceUrl((ModuleDescriptor) mockModuleDescriptor.proxy(), "baz");
        String expectedPrefix ="/" + AbstractFileServerServlet.SERVLET_PATH + "/" + AbstractFileServerServlet.RESOURCE_URL_PREFIX + "/foo%3Abar/baz";
        assertEquals(expectedPrefix, url);
    }

    public void testGetResourceUrlWithEncodedResourceName()
    {
        Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expectAndReturn("getCompleteKey", "foo:bar");
        String url = webResourceManager.getResourceUrl((ModuleDescriptor) mockModuleDescriptor.proxy(), "baz:baz/foobar");
        String expectedPrefix ="/" + AbstractFileServerServlet.SERVLET_PATH + "/" + AbstractFileServerServlet.RESOURCE_URL_PREFIX + "/foo%3Abar/baz%3Abaz/foobar";
        assertEquals(expectedPrefix, url);
    }

    public void testRequireResourceWithoutWriter()
    {
        WebResourceManagerImpl manager = new WebResourceManagerImpl(new FakeWebResourceIntegration(new HashMap()));
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
        }
        catch (IllegalStateException e)
        {
            //expected exception.
        }
    }

    public void testSettingIncludedMode()
    {
        WebResourceManagerImpl manager = new WebResourceManagerImpl(new FakeWebResourceIntegration(new HashMap()));
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