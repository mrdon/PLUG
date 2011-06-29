package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Stack;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultResourceDependencyResolver implements ResourceDependencyResolver
{
    private static final Logger log = LoggerFactory.getLogger(DefaultResourceDependencyResolver.class);

    private final WebResourceIntegration webResourceIntegration;
    private final ResourceBatchingConfiguration batchingConfiguration;

    private String superBatchVersion;
    private LinkedHashSet<String> superBatchResources;

    public DefaultResourceDependencyResolver(WebResourceIntegration webResourceIntegration, ResourceBatchingConfiguration batchingConfiguration)
    {
        this.webResourceIntegration = webResourceIntegration;
        this.batchingConfiguration = batchingConfiguration;
    }

    public LinkedHashSet<String> getSuperBatchDependencies()
    {
        if (!batchingConfiguration.isSuperBatchingEnabled())
        {
            log.warn("Super batching not enabled, but getSuperBatchDependencies() called. Returning empty set.");
            return new LinkedHashSet<String>();
        }

        String version = webResourceIntegration.getSuperBatchVersion();
        if (superBatchVersion == null || !superBatchVersion.equals(version))
        {
            LinkedHashSet<String> webResourceNames = new LinkedHashSet<String>();
            if (batchingConfiguration.getSuperBatchModuleCompleteKeys() != null)
            {
                for (String moduleKey : batchingConfiguration.getSuperBatchModuleCompleteKeys())
                {
                    resolveDependencies(moduleKey, webResourceNames, Collections.emptySet(), new Stack<String>(), null);
                }
            }
            synchronized (this)
            {
                superBatchResources = webResourceNames;
                superBatchVersion = version;
            }
        }
        return new LinkedHashSet<String>(superBatchResources);
    }

    public LinkedHashSet<String> getDependencies(String moduleKey, boolean excludeSuperBatchedResources)
    {
        LinkedHashSet<String> orderedResourceKeys = new LinkedHashSet<String>();
        Set<String> superBatchResources = excludeSuperBatchedResources ? getSuperBatchDependencies() : Collections.<String>emptySet();
        resolveDependencies(moduleKey, orderedResourceKeys, superBatchResources, new Stack<String>(), null);
        return orderedResourceKeys;
    }

    public List<String> getDependenciesInContext(String context)
    {
        return getDependenciesInContext(context, new LinkedHashSet<String>());
    }

    // TODO store/cache this result
    public List<String> getDependenciesInContext(String context, Set<String> skippedResources)
    {
        List<String> contextResources = new ArrayList<String>();
        List<WebResourceModuleDescriptor> descriptors = webResourceIntegration.getPluginAccessor().getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class);
        for (WebResourceModuleDescriptor descriptor : descriptors)
        {
            if (descriptor.getContexts().contains(context))
            {
                LinkedHashSet<String> dependencies = new LinkedHashSet<String>();
                Set<String> superBatchResources = getSuperBatchDependencies();
                resolveDependencies(descriptor.getCompleteKey(), dependencies, superBatchResources, new Stack<String>(), skippedResources);
                for (String dependency : dependencies) {
                    if (!contextResources.contains(dependency))
                    {
                        contextResources.add(dependency);
                    }
                }
            }
        }
        return contextResources;
    }

    /**
     * Adds the resources as well as its dependencies in order to the given ordered set. This method uses recursion
     * to add a resouce's dependent resources also to the set. You should call this method with a new stack
     * passed to the last parameter.
     *
     * Note that resources already in the given super batch will be exluded when resolving dependencies. You
     * should pass in an empty set for the super batch to include super batch resources.
     *
     * @param moduleKey the module complete key to add as well as its dependencies
     * @param orderedResourceKeys an ordered list set where the resources are added in order
     * @param superBatchResources the set of super batch resources to exclude when resolving dependencies
     * @param stack where we are in the dependency tree
     * @param skippedResources if not null, all resources with conditions are skipped and added to this set.
     */
    private void resolveDependencies(final String moduleKey, final LinkedHashSet<String> orderedResourceKeys,
        final Set superBatchResources, final Stack<String> stack, Set<String> skippedResources)
    {
        if (superBatchResources.contains(moduleKey))
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

        if (skippedResources != null && ((WebResourceModuleDescriptor) moduleDescriptor).getCondition() != null)
        {
            skippedResources.add(moduleKey);
            return;
        }
        else if (!((WebResourceModuleDescriptor) moduleDescriptor).shouldDisplay())
        {
            log.debug("Cannot include web resource module {} as its condition fails", moduleDescriptor.getCompleteKey());
            return;
        }

        final List<String> dependencies = ((WebResourceModuleDescriptor) moduleDescriptor).getDependencies();
        if (log.isDebugEnabled())
        {
            log.debug("About to add resource [" + moduleKey + "] and its dependencies: " + dependencies);
        }
        stack.push(moduleKey);
        try
        {
            for (final String dependency : dependencies)
            {
                if (!orderedResourceKeys.contains(dependency))
                {
                    resolveDependencies(dependency, orderedResourceKeys, superBatchResources, stack, skippedResources);
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
