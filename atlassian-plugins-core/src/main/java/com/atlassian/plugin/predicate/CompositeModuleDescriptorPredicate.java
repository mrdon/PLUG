package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;

import java.util.Arrays;

/**
 * Only passes if all of the predicates match
 */
public class CompositeModuleDescriptorPredicate<M> implements ModuleDescriptorPredicate<M>
{
    private final Iterable<ModuleDescriptorPredicate<M>> predicates;

    public CompositeModuleDescriptorPredicate(ModuleDescriptorPredicate<M>... predicates)
    {
        this.predicates = Arrays.asList(predicates);
    }

    public CompositeModuleDescriptorPredicate(Iterable<ModuleDescriptorPredicate<M>> predicates)
    {
        this.predicates = predicates;
    }

    public boolean matches(ModuleDescriptor moduleDescriptor)
    {
        for (ModuleDescriptorPredicate<M> predicate : predicates)
        {
            if (!predicate.matches(moduleDescriptor))
            {
                return false;
            }
        }
        return true;
    }
}
