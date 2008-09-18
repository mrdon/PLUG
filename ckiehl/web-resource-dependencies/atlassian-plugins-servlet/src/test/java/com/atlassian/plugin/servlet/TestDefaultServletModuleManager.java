package com.atlassian.plugin.servlet;

import static com.atlassian.plugin.servlet.DefaultServletModuleManager.sortedInsert;
import static com.atlassian.plugin.servlet.filter.FilterTestUtils.emptyChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.servlet.descriptors.ServletContextListenerModuleDescriptorBuilder;
import com.atlassian.plugin.servlet.descriptors.ServletContextParamDescriptorBuilder;
import com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptorBuilder;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptorBuilder;
import com.atlassian.plugin.servlet.filter.FilterLocation;
import com.atlassian.plugin.servlet.filter.IteratingFilterChain;
import com.atlassian.plugin.servlet.filter.FilterTestUtils.FilterAdapter;
import com.atlassian.plugin.servlet.filter.FilterTestUtils.SoundOffFilter;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

public class TestDefaultServletModuleManager extends TestCase
{
    ServletModuleManager servletModuleManager;
    
    Mock mockPluginEventManager;
    
    public void setUp()
    {
        mockPluginEventManager = new Mock(PluginEventManager.class);
        mockPluginEventManager.expect("register", C.anyArgs(1));
        servletModuleManager = new DefaultServletModuleManager((PluginEventManager) mockPluginEventManager.proxy());
    }
    
    public void testSortedInsertInsertsDistinctElementProperly()
    {
        List<String> list = newList("cat", "dog", "fish", "monkey");
        List<String> endList = newList("cat", "dog", "elephant", "fish", "monkey");
        sortedInsert(list, "elephant", naturalOrder(String.class));
        assertEquals(endList, list); 
    }
    
    public void testSortedInsertInsertsNonDistinctElementProperly()
    {
        List<WeightedValue> list = newList
        (
            new WeightedValue(10, "dog"), new WeightedValue(20, "monkey"), new WeightedValue(20, "tiger"),
            new WeightedValue(30, "fish"), new WeightedValue(100, "cat")
        );
        List<WeightedValue> endList = newList
        (
            new WeightedValue(10, "dog"), new WeightedValue(20, "monkey"), new WeightedValue(20, "tiger"),
            new WeightedValue(20, "elephant"), new WeightedValue(30, "fish"), new WeightedValue(100, "cat")
        );
        sortedInsert(list, new WeightedValue(20, "elephant"), WeightedValue.byWeight);
        assertEquals(endList, list); 
    }
    
    public void testGettingServletWithSimplePath() throws Exception
    {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockServletConfig = new Mock(ServletConfig.class);
        mockServletConfig.expectAndReturn("getServletContext", mockServletContext.proxy());
        
        Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.expectAndReturn("getPathInfo", "/servlet");
        Mock mockHttpServletResponse = new Mock(HttpServletResponse.class);
        
        TestHttpServlet servlet = new TestHttpServlet();
        ServletModuleDescriptor descriptor = new ServletModuleDescriptorBuilder()
            .with(servlet)
            .withPath("/servlet")
            .with(servletModuleManager)
            .build();
        
        servletModuleManager.addServletModule(descriptor);
        
        HttpServlet wrappedServlet = servletModuleManager.getServlet("/servlet", (ServletConfig) mockServletConfig.proxy());
        wrappedServlet.service((HttpServletRequest) mockHttpServletRequest.proxy(), (HttpServletResponse) mockHttpServletResponse.proxy());
        assertTrue(servlet.serviceCalled);
    }
    
    public void testGettingServletWithComplexPath() throws Exception
    {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockServletConfig = new Mock(ServletConfig.class);
        mockServletConfig.expectAndReturn("getServletContext", mockServletContext.proxy());
        
        Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.expectAndReturn("getPathInfo", "/servlet");
        Mock mockHttpServletResponse = new Mock(HttpServletResponse.class);
        
        TestHttpServlet servlet = new TestHttpServlet();
        ServletModuleDescriptor descriptor = new ServletModuleDescriptorBuilder()
            .with(servlet)
            .withPath("/servlet/*")
            .with(servletModuleManager)
            .build();
        
        servletModuleManager.addServletModule(descriptor);
        
        HttpServlet wrappedServlet = servletModuleManager.getServlet("/servlet/this/is/a/test", (ServletConfig) mockServletConfig.proxy());
        wrappedServlet.service((HttpServletRequest) mockHttpServletRequest.proxy(), (HttpServletResponse) mockHttpServletResponse.proxy());
        assertTrue(servlet.serviceCalled);
    }
    
    public void testPluginContextInitParamsGetMerged() throws Exception
    {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockServletConfig = new Mock(ServletConfig.class);
        mockServletConfig.expectAndReturn("getServletContext", mockServletContext.proxy());

        Plugin plugin = new PluginBuilder().build();

        new ServletContextParamDescriptorBuilder()
            .with(plugin)
            .withParam("param.name", "param.value")
            .build();

        // a servlet that will check for param.name to be in the servlet context
        ServletModuleDescriptor servletDescriptor = new ServletModuleDescriptorBuilder()
            .with(plugin)
            .with(new TestHttpServlet()
            {
                @Override
                public void init(ServletConfig servletConfig)
                {
                    assertEquals("param.value", servletConfig.getServletContext().getInitParameter("param.name"));
                }
            })
            .withPath("/servlet")
            .with(servletModuleManager)
            .build();
        servletModuleManager.addServletModule(servletDescriptor);
        
        servletModuleManager.getServlet("/servlet", (ServletConfig) mockServletConfig.proxy());
    }
    
    public void testServletListenerContextInitializedIsCalled() throws Exception
    {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockServletConfig = new Mock(ServletConfig.class);
        mockServletConfig.expectAndReturn("getServletContext", mockServletContext.proxy());
        
        final TestServletContextListener listener = new TestServletContextListener();
        
        Plugin plugin = new PluginBuilder().build();
        
        new ServletContextListenerModuleDescriptorBuilder()
            .with(plugin)
            .with(listener)
            .build();
        
        ServletModuleDescriptor servletDescriptor = new ServletModuleDescriptorBuilder()
            .with(plugin)
            .with(new TestHttpServlet())
            .withPath("/servlet")
            .with(servletModuleManager)
            .build();
        
        servletModuleManager.addServletModule(servletDescriptor);
        servletModuleManager.getServlet("/servlet", (ServletConfig) mockServletConfig.proxy());
        assertTrue(listener.initCalled);
    }
    
    public void testServletListenerContextFilterAndServletUseTheSameServletContext() throws Exception
    {
        Plugin plugin = new PluginBuilder().build();

        final AtomicReference<ServletContext> contextRef = new AtomicReference<ServletContext>();
        // setup a context listener to capture the context
        new ServletContextListenerModuleDescriptorBuilder()
            .with(plugin)
            .with(new TestServletContextListener()
            {
                @Override
                public void contextInitialized(ServletContextEvent event)
                {
                    contextRef.set(event.getServletContext());
                }
            })
            .build();
        
        // a servlet that checks that the context is the same for it as it was for the context listener
        ServletModuleDescriptor servletDescriptor = new ServletModuleDescriptorBuilder()
            .with(plugin)
            .with(new TestHttpServlet()
            {
                @Override
                public void init(ServletConfig servletConfig)
                {
                    assertSame(contextRef.get(), servletConfig.getServletContext());
                }
            })
            .withPath("/servlet")
            .with(servletModuleManager)
            .build();
        servletModuleManager.addServletModule(servletDescriptor);
        
        // a filter that checks that the context is the same for it as it was for the context listener
        ServletFilterModuleDescriptor filterDescriptor = new ServletFilterModuleDescriptorBuilder()
            .with(plugin)
            .with(new FilterAdapter()
            {
                @Override
                public void init(FilterConfig filterConfig)
                {
                    assertSame(contextRef.get(), filterConfig.getServletContext());
                }
            })
            .withPath("/*")
            .with(servletModuleManager)
            .build();
        servletModuleManager.addFilterModule(filterDescriptor);
        
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);

        // get a servlet, this will initialize the servlet context for the first time in addition to the servlet itself.
        // if the servlet doesn't get the same context as the context listener did, the assert will fail
        Mock mockServletConfig = new Mock(ServletConfig.class);
        mockServletConfig.expectAndReturn("getServletContext", mockServletContext.proxy());
        servletModuleManager.getServlet("/servlet", (ServletConfig) mockServletConfig.proxy());
        
        // get the filters, if the filter doesn't get the same context as the context listener did, the assert will fail
        Mock mockFilterConfig = new Mock(FilterConfig.class);
        mockFilterConfig.expectAndReturn("getServletContext", mockServletContext.proxy());
        servletModuleManager.getFilters(FilterLocation.bottom, "/servlet", (FilterConfig) mockFilterConfig.proxy());
    }
    
    public void testFiltersWithSameLocationAndWeightInTheSamePluginAppearInTheOrderTheyAreDeclared() throws Exception
    {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.matchAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockFilterConfig = new Mock(FilterConfig.class);
        mockFilterConfig.matchAndReturn("getServletContext", mockServletContext.proxy());

        Plugin plugin = new PluginBuilder().build();
        
        List<Integer> filterCallOrder = new LinkedList<Integer>();
        ServletFilterModuleDescriptor d1 = new ServletFilterModuleDescriptorBuilder()
            .with(plugin)
            .withKey("filter-1")
            .with(new SoundOffFilter(filterCallOrder, 1))
            .withPath("/*")
            .build();
        servletModuleManager.addFilterModule(d1);
        
        ServletFilterModuleDescriptor d2 = new ServletFilterModuleDescriptorBuilder()
            .with(plugin)
            .withKey("filter-2")
            .with(new SoundOffFilter(filterCallOrder, 2))
            .withPath("/*")
            .build();
        servletModuleManager.addFilterModule(d2);
        
        Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.matchAndReturn("getPathInfo", "/servlet");
        Mock mockHttpServletResponse = new Mock(HttpServletResponse.class);
        
        Iterable<Filter> filters = servletModuleManager.getFilters(FilterLocation.bottom, "/some/path", (FilterConfig) mockFilterConfig.proxy());
        FilterChain chain = new IteratingFilterChain(filters.iterator(), emptyChain);
        
        chain.doFilter((HttpServletRequest) mockHttpServletRequest.proxy(), (HttpServletResponse) mockHttpServletResponse.proxy());
        assertEquals(newList(1, 2, 2, 1), filterCallOrder);
    }        

    static class TestServletContextListener implements ServletContextListener
    {
        boolean initCalled = false;
        
        public void contextInitialized(ServletContextEvent event)
        {
            initCalled = true;
        }

        public void contextDestroyed(ServletContextEvent event) {}
    }
    
    static class TestHttpServlet extends HttpServlet
    {
        boolean serviceCalled = false;
        
        @Override
        public void service(ServletRequest request, ServletResponse response)
        {
            serviceCalled = true;
        }
    }

    static final class WeightedValue
    {
        final int weight;
        final String value;
        
        WeightedValue(int weight, String value)
        {
            this.weight = weight;
            this.value = value;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (!(o instanceof WeightedValue))
                return false;
            WeightedValue rhs = (WeightedValue) o;
            return weight == rhs.weight && value.equals(rhs.value);
        }
        
        @Override
        public String toString()
        {
            return "[" + weight + ", " + value + "]";
        }
        
        static final Comparator<WeightedValue> byWeight = new Comparator<WeightedValue>()
        {
            public int compare(WeightedValue o1, WeightedValue o2)
            {
                return Integer.valueOf(o1.weight).compareTo(o2.weight);
            }
        };
    }
    
    static <T> List<T> newList(T... elements)
    {
        List<T> list = new ArrayList<T>();
        for (T e : elements)
        {
            list.add(e);
        }
        return list;
    }
    
    static <T extends Comparable<T>> Comparator<T> naturalOrder(Class<T> type)
    {
        return new Comparator<T>()
        {
            public int compare(T o1, T o2)
            {
                return o1.compareTo(o2);
            }
        };
    }    
}
