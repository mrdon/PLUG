package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Stack;

public class DefaultResourceDependencyResolver implements ResourceDependencyResolver
{
    private static final Log log = LogFactory.getLog(DefaultResourceDependencyResolver.class);

    private final WebResourceIntegration webResourceIntegration;
    private final ResourceBatchingConfiguration batchingConfiguration;

    private String superBatchVersion;
    private LinkedHashSet<String> superBatchResources;

    public DefaultResourceDependencyResolver(WebResourceIntegration webResourceIntegration)
    {
        this(webResourceIntegration, new DefaultResourceBatchingConfiguration());
    }

    public DefaultResourceDependencyResolver(WebResourceIntegration webResourceIntegration, ResourceBatchingConfiguration batchingConfiguration)
    {
        this.webResourceIntegration = webResourceIntegration;
        this.batchingConfiguration = batchingConfiguration;
    }

    public LinkedHashSet<String> getSuperBatchDependencies()
    {
        String version = webResourceIntegration.getSuperBatchVersion();
        if (superBatchVersion == null || !superBatchVersion.equals(version))
        {
            LinkedHashSet<String> webResourceNames = new LinkedHashSet<String>();
            for (String moduleKey : batchingConfiguration.getSuperBatchModuleCompleteKeys())
            {
                resolveDependencies(moduleKey, webResourceNames, false, new Stack<String>());
            }

            synchronized (this)
            {
                superBatchResources = webResourceNames;
                superBatchVersion = webResourceIntegration.getSuperBatchVersion();
            }
        }
        return superBatchResources;
    }

    public LinkedHashSet<String> getDependencies(String moduleKey, boolean excludeSuperBatchedResources)
    {
        LinkedHashSet<String> orderedResourceKeys = new LinkedHashSet<String>();
        resolveDependencies(moduleKey, orderedResourceKeys, excludeSuperBatchedResources, new Stack<String>());
        return orderedResourceKeys;
    }

    /**
     * Adds the resources as well as its dependencies in order to the given set. This method uses recursion
     * to add a resouce's dependent resources also to the set. You should call this method with a new stack
     * passed to the last parameter.
     *
     * @param moduleKey the module complete key to add as well as its dependencies
     * @param orderedResourceKeys an ordered list set where the resources are added in order
     * @param excludeSuperBatchedResources true if resources contained in the superbatch should be excluded
     * @param stack where we are in the dependency tree
     */
    private void resolveDependencies(String moduleKey, LinkedHashSet<String> orderedResourceKeys,
        boolean excludeSuperBatchedResources, Stack<String> stack)
    {
        if (excludeSuperBatchedResources && getSuperBatchDependencies().contains(moduleKey))
        {
            log.debug("Not requiring resource: " + moduleKey + " because it is part of a super-batch");
            return;
        }
        if (stack.contains(moduleKey))
        {
            log.warn("Cyclic plugin resource dependency has been detected with: " + moduleKey + "\n" + "Stack trace: " + stack);
            return;
        }

        final ModuleDescriptor<?> moduleDescriptor = webResourceIntegration.getPluginAccessor().getEnabledPluginModule(moduleKey);
        if (!(moduleDescriptor instanceof WebResourceModuleDescriptor))
        {
            if (webResourceIntegration.getPluginAccessor().getPluginModule(moduleKey) != null)
                log.warn("Cannot include disabled web resource module: " + moduleKey);
            else
                log.warn("Cannot find web resource module for: " + moduleKey);
            return;
        }

        final List<String> dependencies = ((WebResourceModuleDescriptor) moduleDescriptor).getDependencies();
        if (log.isDebugEnabled())
            log.debug("About to add resource [" + moduleKey + "] and its dependencies: " + dependencies);

        stack.push(moduleKey);
        try
        {
            for (final String dependency : dependencies)
            {
                if (!orderedResourceKeys.contains(dependency))
                {
                    resolveDependencies(dependency, orderedResourceKeys, excludeSuperBatchedResources, stack);
                }
            }
        }
        finally
        {
            stack.pop();
        }
        orderedResourceKeys.add(moduleKey);
    }
}
