package com.atlassian.plugin.servlet.download.plugin;

import com.atlassian.plugin.module.ClassModuleCreator;
import com.atlassian.plugin.module.DefaultModuleClassFactory;
import com.atlassian.plugin.module.ModuleClassFactory;
import com.atlassian.plugin.module.ModuleCreator;
import junit.framework.TestCase;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;
import com.atlassian.plugin.event.events.PluginModuleDisabledEvent;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.servlet.DownloadStrategy;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.mockobjects.dynamic.Mock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class TestPluggableDownloadStrategy extends TestCase
{
    private PluggableDownloadStrategy strategy;

    protected void setUp() throws Exception
    {
        super.setUp();
        strategy = new PluggableDownloadStrategy(new DefaultPluginEventManager());
    }

    public void testRegister() throws Exception
    {
        strategy.register("monkey.key", new StubDownloadStrategy("/monkey", "Bananas"));

        assertTrue(strategy.matches("/monkey/something"));

        StringWriter result = new StringWriter();
        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expectAndReturn("getWriter", new PrintWriter(result));
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getRequestURI", "/monkey/something");

        strategy.serveFile((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
        assertEquals("Bananas\n", result.toString());
    }

    public void testUnregister() throws Exception
    {
        strategy.register("monkey.key", new StubDownloadStrategy("/monkey", "Bananas"));
        strategy.unregister("monkey.key");

        assertFalse(strategy.matches("/monkey/something"));
    }

    protected ModuleClassFactory getDefaultModuleClassFactory()
    {
        final List<ModuleCreator> moduleCreators = new ArrayList<ModuleCreator>();
        moduleCreators.add(new ClassModuleCreator(new DefaultHostContainer()));
        return new DefaultModuleClassFactory(moduleCreators);
    }

    public void testPluginModuleEnabled() throws Exception
    {

        ModuleDescriptor module = new DownloadStrategyModuleDescriptor(getDefaultModuleClassFactory()) {
            public String getCompleteKey()
            {
                return "jungle.plugin:lion-strategy";
            }

            public DownloadStrategy getModule()
            {
                return new StubDownloadStrategy("/lion", "ROAR!");
            }
        };

        strategy.pluginModuleEnabled(new PluginModuleEnabledEvent(module));

        assertTrue(strategy.matches("/lion/something"));

        StringWriter result = new StringWriter();
        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expectAndReturn("getWriter", new PrintWriter(result));
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getRequestURI", "/lion/something");

        strategy.serveFile((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
        assertEquals("ROAR!\n", result.toString());
    }

    public void testPluginModuleDisabled() throws Exception
    {
        ModuleDescriptor module = new DownloadStrategyModuleDescriptor(getDefaultModuleClassFactory()) {
            public String getCompleteKey()
            {
                return "jungle.plugin:lion-strategy";
            }

            public DownloadStrategy getModule()
            {
                return new StubDownloadStrategy("/lion", "ROAR!");
            }
        };

        strategy.pluginModuleEnabled(new PluginModuleEnabledEvent(module));
        assertTrue(strategy.matches("/lion/something"));

        strategy.pluginModuleDisabled(new PluginModuleDisabledEvent(module));
        assertFalse(strategy.matches("/lion/something"));
    }

    public void testUnregisterPluginModule() throws Exception
    {
        ModuleDescriptor module = new DownloadStrategyModuleDescriptor(getDefaultModuleClassFactory()) {
            public String getCompleteKey()
            {
                return "jungle.plugin:lion-strategy";
            }

            public DownloadStrategy getModule()
            {
                return new StubDownloadStrategy("/lion", "ROAR!");
            }
        };

        strategy.pluginModuleEnabled(new PluginModuleEnabledEvent(module));
        assertTrue(strategy.matches("/lion/something"));

        strategy.unregister("jungle.plugin:lion-strategy");
        assertFalse(strategy.matches("/lion/something"));
    }

    private static class StubDownloadStrategy implements DownloadStrategy
    {
        private final String urlPattern;
        private final String output;

        public StubDownloadStrategy(String urlPattern, String output)
        {
            this.urlPattern = urlPattern;
            this.output = output;
        }

        public boolean matches(String urlPath)
        {
            return urlPath.contains(urlPattern);
        }

        public void serveFile(HttpServletRequest request, HttpServletResponse response) throws DownloadException
        {
            try
            {
                response.getWriter().println(output);
            }
            catch (IOException e)
            {
                throw new DownloadException(e);
            }
        }
    }
}
