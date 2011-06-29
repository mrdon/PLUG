package com.atlassian.plugin.webresource;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

interface ResourceDependencyResolver
{
    /**
     * Returns an ordered set of the super batch resources and its dependencies.
     * Implementations are expected to use the {@link ResourceBatchingConfiguration} provided.
     * @return an ordered set of the super batch resources and its dependencies.
     */
    public LinkedHashSet<String> getSuperBatchDependencies();

    /**
     * Returns an ordered set of the given resource and its dependencies. To exclude resource dependencies
     * in the super batch, pass excludeSuperBatchedResources as true.
     *
     * @param moduleKey the complete module key of the web resource to retrieve dependencies for
     * @param excludeSuperBatchedResources whether or not to exclude resources that are part of the super batch.
     * @return an ordered set of the given resource and its dependencies
     */
    public LinkedHashSet<String> getDependencies(String moduleKey, boolean excludeSuperBatchedResources);

    /**
     * Returns an ordered list of the resources in a given context and its dependencies.
     * Dependencies with conditions are not included.
     * @param context - the context to retrieve dependencies from.
     * @return an ordered list of the resources in a given context and its dependencies.
     */
    public List<String> getDependenciesInContext(String context);

    /**
     * Returns an ordered list of the resources in a given context and its dependencies.
     * Dependencies with conditions are not included and are added to the parameter skippedResources
     * @param context - the context to retrieve dependencies from.
     * @param skippedResources - a list that all the resources that are skipped are added to.
     * @return an ordered list of the resources in a given context and its dependencies.
     */
    public List<String> getDependenciesInContext(String context, Set<String> skippedResources);
}
