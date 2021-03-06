package com.atlassian.plugin.webresource;

import com.atlassian.util.concurrent.NotNull;

import java.util.Set;

interface ResourceDependencyResolver
{
    /**
     * Returns an ordered set of the super batch resources and its dependencies.
     * If no super batch resources are defined an empty set is returned.
     * Implementations are expected to use the {@link ResourceBatchingConfiguration} provided.
     * @return an ordered set of the super batch resources and its dependencies.
     */
    public Iterable<WebResourceModuleDescriptor> getSuperBatchDependencies();

    /**
     * Returns an ordered set of the given resource and its dependencies. To exclude resource dependencies
     * in the super batch, pass excludeSuperBatchedResources as true.
     * If the resources cannot be resolved an empty set is returned.
     * @param moduleKey the complete module key of the web resource to retrieve dependencies for
     * @param excludeSuperBatchedResources whether or not to exclude resources that are part of the super batch.
     * @return an ordered set of the given resource and its dependencies
     */
    public Iterable<WebResourceModuleDescriptor> getDependencies(String moduleKey, boolean excludeSuperBatchedResources);

    /**
     * Returns an ordered list of the resources in a given context and its dependencies.
     * Dependencies with conditions are not included.
     * If no resources are defined for the given context an empty set is returned.
     * @param context - the context to retrieve dependencies from.
     * @return an ordered list of the resources in a given context and its dependencies.
     * @since 2.9.0
     */
    public Iterable<WebResourceModuleDescriptor> getDependenciesInContext(@NotNull String context);

    /**
     * Returns an ordered list of the resources in a given context and its dependencies.
     * Dependencies with conditions are not included and are added to the parameter skippedResources
     * If no resources are defined for the given context an empty set is returned.
     * @param context - the context to retrieve dependencies from.
     * @param skippedResources - a list that all the resources that are skipped are added to.
     * @return an ordered list of the resources in a given context and its dependencies.
     * @since 2.9.0
     */
    public Iterable<WebResourceModuleDescriptor> getDependenciesInContext(@NotNull String context, @NotNull Set<String> skippedResources);
}
