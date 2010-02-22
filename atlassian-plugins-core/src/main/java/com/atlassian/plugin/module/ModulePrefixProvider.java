package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * TODO: Document this class / interface here
 */
public interface ModulePrefixProvider
{
    boolean supportsPrefix(String prefix);

    /**
     *
     * @param name
     * @param moduleDescriptor
     * @param <T>
     * @return Can return null
     */
    <T> T create(String name, ModuleDescriptor<T> moduleDescriptor);

}
