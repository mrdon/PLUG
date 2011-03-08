package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * Interface used to match plugin modules according to implementation specific rules.
 *
 * @since 0.17
 */
public interface ModuleDescriptorPredicate<T>
{
    /**
    * <p>Will match a plugin module according to implementation rules.<p>
    * <p>This method must not change the state of the module nor its plugin .</p>
    *
    * @param moduleDescriptor the {@link ModuleDescriptor} to test against.
    * @return <code>true</code> if the module matches the predicate, <code>false</code> otherwise.
    */
    boolean matches(final ModuleDescriptor<? extends T> moduleDescriptor);
}
