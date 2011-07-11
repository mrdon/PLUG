package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Stack;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Iterables.contains;

class DefaultResourceDependencyResolver implements ResourceDependencyResolver
{
    private static final Logger log = LoggerFactory.getLogger(DefaultResourceDependencyResolver.class);

    private final WebResourceIntegration webResourceIntegration;
    private final ResourceBatchingConfiguration batchingConfiguration;

    private String superBatchVersion;
    private LinkedHashMap<String, WebResourceModuleDescriptor> superBatchResources;

    public DefaultResourceDependencyResolver(WebResourceIntegration webResourceIntegration, ResourceBatchingConfiguration batchingConfiguration)
    {
        this.webResourceIntegration = webResourceIntegration;
        this.batchingConfiguration = batchingConfiguration;
    }

    public Iterable<WebResourceModuleDescriptor> getSuperBatchDependencies()
    {
        if (!batchingConfiguration.isSuperBatchingEnabled())
        {
            log.warn("Super batching not enabled, but getSuperBatchDependencies() called. Returning empty set.");
            return Collections.emptyList();
        }

        return getSuperBatchResourcesMap().values();
    }

    private Iterable<String> getSuperBatchDependencyKeys()
    {
        if (!batchingConfiguration.isSuperBatchingEnabled())
        {
            log.warn("Super batching not enabled, but getSuperBatchDependencies() called. Returning empty set.");
            return Collections.emptyList();
        }

        return getSuperBatchResourcesMap().keySet();
    }

    private LinkedHashMap<String, WebResourceModuleDescriptor> getSuperBatchResourcesMap()
    {
        String version = webResourceIntegration.getSuperBatchVersion();
        if (superBatchVersion == null || !superBatchVersion.equals(version))
        {
            // The linked hash map ensures that order is preserved
            LinkedHashMap<String, WebResourceModuleDescriptor> webResources = new LinkedHashMap<String, WebResourceModuleDescriptor>();
            if (batchingConfiguration.getSuperBatchModuleCompleteKeys() != null)
            {
                for (String moduleKey : batchingConfiguration.getSuperBatchModuleCompleteKeys())
                {
                    resolveDependencies(moduleKey, webResources, Collections.<String>emptyList(), new Stack<String>(), null);
                }
            }
            synchronized (this)
            {
                superBatchResources = webResources;
                superBatchVersion = version;
            }
        }

        return superBatchResources;
    }

    public Iterable<WebResourceModuleDescriptor> getDependencies(String moduleKey, boolean excludeSuperBatchedResources)
    {
        LinkedHashMap<String, WebResourceModuleDescriptor> orderedResources = new LinkedHashMap<String, WebResourceModuleDescriptor>();
        Iterable<String> superBatchResources = excludeSuperBatchedResources ? getSuperBatchDependencyKeys() : Collections.<String>emptyList();
        resolveDependencies(moduleKey, orderedResources, superBatchResources, new Stack<String>(), null);
        return orderedResources.values();
    }

    public Iterable<WebResourceModuleDescriptor> getDependenciesInContext(String context)
    {
        return getDependenciesInContext(context, new LinkedHashSet<String>());
    }

    public Iterable<WebResourceModuleDescriptor> getDependenciesInContext(String context, Set<String> skippedResources)
    {
        List<WebResourceModuleDescriptor> contextResources = new ArrayList<WebResourceModuleDescriptor>();
        List<WebResourceModuleDescriptor> descriptors = webResourceIntegration.getPluginAccessor().getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class);
        for (WebResourceModuleDescriptor descriptor : descriptors)
        {
            if (descriptor.getContexts().contains(context))
            {
                LinkedHashMap<String, WebResourceModuleDescriptor> dependencies = new LinkedHashMap<String, WebResourceModuleDescriptor>();
                Iterable<String> superBatchResources = getSuperBatchDependencyKeys();
                resolveDependencies(descriptor.getCompleteKey(), dependencies, superBatchResources, new Stack<String>(), skippedResources);
                for (WebResourceModuleDescriptor dependency : dependencies.values()) {
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
    private void resolveDependencies(final String moduleKey, final LinkedHashMap<String, WebResourceModuleDescriptor> orderedResourceKeys,
        final Iterable<String> superBatchResources, final Stack<String> stack, Set<String> skippedResources)
    {
        if (contains(superBatchResources, moduleKey))
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

        WebResourceModuleDescriptor webResourceModuleDescriptor = (WebResourceModuleDescriptor) moduleDescriptor;

        if (skippedResources != null && webResourceModuleDescriptor.getCondition() != null)
        {
            skippedResources.add(moduleKey);
            return;
        }
        else if (!webResourceModuleDescriptor.shouldDisplay())
        {
            log.debug("Cannot include web resource module {} as its condition fails", moduleDescriptor.getCompleteKey());
            return;
        }

        final List<String> dependencies = webResourceModuleDescriptor.getDependencies();
        if (log.isDebugEnabled())
        {
            log.debug("About to add resource [" + moduleKey + "] and its dependencies: " + dependencies);
        }
        stack.push(moduleKey);
        try
        {
            for (final String dependency : dependencies)
            {
                if (orderedResourceKeys.get(dependency) == null)
                {
                    resolveDependencies(dependency, orderedResourceKeys, superBatchResources, stack, skippedResources);
                }
            }
        }
        finally
        {
            stack.pop();
        }
        orderedResourceKeys.put(moduleKey, webResourceModuleDescriptor);
    }
}
