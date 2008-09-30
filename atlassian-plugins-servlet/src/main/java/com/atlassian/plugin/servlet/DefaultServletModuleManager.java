package com.atlassian.plugin.servlet;

import static com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptor.byWeight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.servlet.descriptors.ServletContextListenerModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletContextParamModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.filter.DelegatingPluginFilter;
import com.atlassian.plugin.servlet.filter.FilterLocation;
import com.atlassian.plugin.servlet.filter.PluginFilterConfig;
import com.atlassian.plugin.servlet.util.ClassLoaderStack;
import com.atlassian.plugin.servlet.util.DefaultPathMapper;
import com.atlassian.plugin.servlet.util.LazyLoadedReference;
import com.atlassian.plugin.servlet.util.PathMapper;

/**
 * A simple servletModuleManager to track and retrieve the loaded servlet plugin modules.
 */
public class DefaultServletModuleManager implements ServletModuleManager
{
    PathMapper servletMapper = new DefaultPathMapper();
    Map<String, ServletModuleDescriptor> servletDescriptors = new HashMap<String, ServletModuleDescriptor>();
    ConcurrentMap<String, LazyLoadedReference<HttpServlet>> servletRefs = new ConcurrentHashMap<String, LazyLoadedReference<HttpServlet>>();

    PathMapper filterMapper = new DefaultPathMapper();
    Map<String, ServletFilterModuleDescriptor> filterDescriptors = new HashMap<String, ServletFilterModuleDescriptor>();
    ConcurrentMap<String, LazyLoadedReference<Filter>> filterRefs = new ConcurrentHashMap<String, LazyLoadedReference<Filter>>();
    
    ConcurrentMap<Plugin, LazyLoadedReference<ServletContext>> pluginContextRefs = new ConcurrentHashMap<Plugin, LazyLoadedReference<ServletContext>>();
    
    public DefaultServletModuleManager(PluginEventManager pluginEventManager)
    {
        pluginEventManager.register(this);
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.servlet.ServletModuleManager#addModule(com.atlassian.plugin.servlet.ServletModuleDescriptor)
     */
    public void addServletModule(ServletModuleDescriptor descriptor)
    {
        servletDescriptors.put(descriptor.getCompleteKey(), descriptor);

        for (String path : descriptor.getPaths())
        {
            servletMapper.put(descriptor.getCompleteKey(), path);
        }
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.servlet.ServletModuleManager#getServlet(java.lang.String, javax.servlet.ServletConfig)
     */
    public HttpServlet getServlet(String path, final ServletConfig servletConfig) throws ServletException
    {
        String completeKey = servletMapper.get(path);

        if (completeKey == null)
        {
            return null;
        }
        ServletModuleDescriptor descriptor = servletDescriptors.get(completeKey);
        if (descriptor == null)
        {
            return null;
        }

        return getServlet(descriptor, servletConfig);
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.servlet.ServletModuleManager#removeModule(com.atlassian.plugin.servlet.ServletModuleDescriptor)
     */
    public void removeServletModule(ServletModuleDescriptor descriptor)
    {
        servletDescriptors.remove(descriptor.getCompleteKey());
        servletMapper.put(descriptor.getCompleteKey(), null);

        LazyLoadedReference<HttpServlet> servletRef = servletRefs.remove(descriptor.getCompleteKey());
        if (servletRef != null)
        {
            servletRef.get().destroy();
        }
    }

    public void addFilterModule(ServletFilterModuleDescriptor descriptor)
    {
        filterDescriptors.put(descriptor.getCompleteKey(), descriptor);

        for (String path : descriptor.getPaths())
        {
            filterMapper.put(descriptor.getCompleteKey(), path);
        }
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.servlet.ServletModuleManager#getFilters(com.atlassian.plugin.servlet.FilterLocation, java.lang.String, javax.servlet.FilterConfig)
     */
    public Iterable<Filter> getFilters(FilterLocation location, String path, final FilterConfig filterConfig) throws ServletException
    {
        List<ServletFilterModuleDescriptor> matchingFilterDescriptors = new ArrayList<ServletFilterModuleDescriptor>();
        for (String completeKey : filterMapper.getAll(path))
        {
            final ServletFilterModuleDescriptor descriptor = filterDescriptors.get(completeKey);
            if (location.equals(descriptor.getLocation()))
            {
                sortedInsert(matchingFilterDescriptors, descriptor, byWeight);
            }
        }
        List<Filter> filters = new LinkedList<Filter>();
        for (final ServletFilterModuleDescriptor descriptor : matchingFilterDescriptors)
        {
            filters.add(getFilter(descriptor, filterConfig));
        }
        return filters;
    }

    static <T> void sortedInsert(List<T> list, final T e, Comparator<T> comparator)
    {
        int insertIndex = Collections.binarySearch(list, e, comparator);
        if (insertIndex < 0)
        {
            // no entry already there, so the insertIndex is the negative value of where it should be inserted 
            insertIndex = -insertIndex - 1;
        }
        else
        {
            // there is already a value at that position, so we need to find the next available spot for it
            while (insertIndex < list.size() && comparator.compare(list.get(insertIndex), e) == 0)
            {
                insertIndex++;
            }
        }
        list.add(insertIndex, e);
    }

    public void removeFilterModule(ServletFilterModuleDescriptor descriptor)
    {
        filterDescriptors.remove(descriptor.getCompleteKey());
        filterMapper.put(descriptor.getCompleteKey(), null);

        LazyLoadedReference<Filter> filterRef = filterRefs.remove(descriptor.getCompleteKey());
        if (filterRef != null)
        {
            filterRef.get().destroy();
        }
    }
    
    /**
     * Call the plugins servlet context listeners contextDestroyed methods and cleanup any servlet contexts that are 
     * associated with the plugin that was disabled.
     */
    @PluginEventListener
    public void onPluginDisabled(PluginDisabledEvent event)
    {
        Plugin plugin = event.getPlugin();
        LazyLoadedReference<ServletContext> context = pluginContextRefs.remove(plugin);
        if (context == null)
        {
            return;
        }
        
        for (ServletContextListenerModuleDescriptor descriptor : findModuleDescriptorsByType(ServletContextListenerModuleDescriptor.class, plugin))
        {
            descriptor.getModule().contextDestroyed(new ServletContextEvent(context.get()));
        }
    }

    /**
     * Returns a wrapped Servlet for the servlet module.  If a wrapped servlet for the module has not been 
     * created yet, we create one using the servletConfig.
     * <p/>
     * Note: We use a map of lazily loaded references to the servlet so that only one can ever be created and 
     * initialized for each module descriptor.
     * 
     * @param descriptor
     * @param servletConfig
     * @return
     */
    private HttpServlet getServlet(final ServletModuleDescriptor descriptor, final ServletConfig servletConfig)
    {
        // check for an existing reference, if there is one it's either in the process of loading, in which case
        // servletRef.get() below will block until it's available, otherwise we go about creating a new ref to use
        LazyLoadedReference<HttpServlet> servletRef = servletRefs.get(descriptor.getCompleteKey());
        if (servletRef == null)
        {
            // if there isn't an existing reference, create one.
            ServletContext servletContext = getWrappedContext(descriptor.getPlugin(), servletConfig.getServletContext());
            servletRef = new LazyLoadedServletReference(descriptor, servletContext);
            
            // check that another thread didn't beat us to the punch of creating a lazy reference.  if it did, we
            // want to use that so there is only ever one reference 
            if (servletRefs.putIfAbsent(descriptor.getCompleteKey(), servletRef) != null)
            {
                servletRef = servletRefs.get(descriptor.getCompleteKey());
            }
        }
        return servletRef.get();
    }
    
    /**
     * Returns a wrapped Filter for the filter module.  If a wrapped filter for the module has not been 
     * created yet, we create one using the filterConfig.
     * <p/>
     * Note: We use a map of lazily loaded references to the filter so that only one can ever be created and 
     * initialized for each module descriptor.
     * 
     * @param descriptor
     * @param filterConfig
     * @return
     */
    private Filter getFilter(final ServletFilterModuleDescriptor descriptor, final FilterConfig filterConfig)
    {
        // check for an existing reference, if there is one it's either in the process of loading, in which case
        // filterRef.get() below will block until it's available, otherwise we go about creating a new ref to use
        LazyLoadedReference<Filter> filterRef = filterRefs.get(descriptor.getCompleteKey());
        if (filterRef == null)
        {
            // if there isn't an existing reference, create one.
            ServletContext servletContext = getWrappedContext(descriptor.getPlugin(), filterConfig.getServletContext());
            filterRef = new LazyLoadedFilterReference(descriptor, servletContext);
            
            // check that another thread didn't beat us to the punch of creating a lazy reference.  if it did, we
            // want to use that so there is only ever one reference 
            if (filterRefs.putIfAbsent(descriptor.getCompleteKey(), filterRef) != null)
            {
                filterRef = filterRefs.get(descriptor.getCompleteKey());
            }
        }
        return filterRef.get();
    }

    /**
     * Returns a wrapped ServletContext for the plugin.  If a wrapped servlet context for the plugin has not been 
     * created yet, we create using the baseContext, any context params specified in the plugin and initialize any
     * context listeners the plugin may define.
     * <p/>
     * Note: We use a map of lazily loaded references to the context so that only one can ever be created for each
     * plugin. 
     *  
     * @param plugin Plugin for whom we're creating a wrapped servlet context.
     * @param baseContext The applications base servlet context which we will be wrapping.
     * @return A wrapped, fully initialized servlet context that can be used for all the plugins filters and servlets.
     */
    private ServletContext getWrappedContext(Plugin plugin, ServletContext baseContext)
    {
        LazyLoadedReference<ServletContext> pluginContextRef = pluginContextRefs.get(plugin);
        if (pluginContextRef == null)
        {
            pluginContextRef = new LazyLoadedContextReference(plugin, baseContext);
            if (pluginContextRefs.putIfAbsent(plugin, pluginContextRef) != null)
            {
                pluginContextRef = pluginContextRefs.get(plugin);
            }
        }
        return pluginContextRef.get();
    }

    private static final class LazyLoadedFilterReference extends LazyLoadedReference<Filter>
    {
        private final ServletFilterModuleDescriptor descriptor;
        private final ServletContext servletContext;

        private LazyLoadedFilterReference(ServletFilterModuleDescriptor descriptor, ServletContext servletContext)
        {
            this.descriptor = descriptor;
            this.servletContext = servletContext;
        }

        @Override
        protected Filter create() throws Exception
        {
            Filter filter = new DelegatingPluginFilter(descriptor);
            filter.init(new PluginFilterConfig(descriptor, servletContext));
            return filter;
        }
    }

    private static final class LazyLoadedServletReference extends LazyLoadedReference<HttpServlet>
    {
        private final ServletModuleDescriptor descriptor;
        private final ServletContext servletContext;

        private LazyLoadedServletReference(ServletModuleDescriptor descriptor, ServletContext servletContext)
        {
            this.descriptor = descriptor;
            this.servletContext = servletContext;
        }

        @Override
        protected HttpServlet create() throws Exception
        {
            HttpServlet servlet = new DelegatingPluginServlet(descriptor);
            servlet.init(new PluginServletConfig(descriptor, servletContext));
            return servlet;
        }
    }
    
    private static final class LazyLoadedContextReference extends LazyLoadedReference<ServletContext>
    {
        private final Plugin plugin;
        private final ServletContext baseContext;
        
        private LazyLoadedContextReference(Plugin plugin, ServletContext baseContext)
        {
            this.plugin = plugin;
            this.baseContext = baseContext;
        }
        
        @Override
        protected ServletContext create() throws Exception
        {
            ConcurrentMap<String, Object> contextAttributes = new ConcurrentHashMap<String, Object>();
            Map<String, String> initParams = mergeInitParams(baseContext, plugin);
            ServletContext context = new PluginServletContextWrapper(plugin, baseContext, contextAttributes, initParams);

            ClassLoaderStack.push(plugin.getClassLoader());
            try
            {
                for (ServletContextListenerModuleDescriptor descriptor : findModuleDescriptorsByType(ServletContextListenerModuleDescriptor.class, plugin))
                {
                    descriptor.getModule().contextInitialized(new ServletContextEvent(context));
                }
            } 
            finally
            {
                ClassLoaderStack.pop();
            }
            
            return context;
        }
        
        private Map<String, String> mergeInitParams(ServletContext baseContext, Plugin plugin)
        {
            Map<String, String> mergedInitParams = new HashMap<String, String>();
            for (Enumeration<String> e = baseContext.getInitParameterNames(); e.hasMoreElements(); )
            {
                String paramName = e.nextElement();
                mergedInitParams.put(paramName, baseContext.getInitParameter(paramName));
            }
            for (ServletContextParamModuleDescriptor descriptor : findModuleDescriptorsByType(ServletContextParamModuleDescriptor.class, plugin))
            {
                mergedInitParams.put(descriptor.getParamName(), descriptor.getParamValue());
            }
            return Collections.unmodifiableMap(mergedInitParams);
        }
    }
    
    static <T extends ModuleDescriptor<?>> Iterable<T> findModuleDescriptorsByType(Class<T> type, Plugin plugin)
    {
        Set<T> descriptors = new HashSet<T>();
        for (ModuleDescriptor<?> descriptor : plugin.getModuleDescriptors())
        {
            if (type.isAssignableFrom(descriptor.getClass()))
            {
                descriptors.add((T) descriptor);
            }
        }
        return descriptors;
    }
}
