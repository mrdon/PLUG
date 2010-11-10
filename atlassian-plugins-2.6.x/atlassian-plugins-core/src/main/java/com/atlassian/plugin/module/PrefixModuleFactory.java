package com.atlassian.plugin.module;

/**
 * A module factory that is matched when its prefix is matched, and therefore, relies an a delegating module factory
 * that determines the prefix somehow.
 *
 * @since 2.5.0
 * @see {@link PrefixDelegatingModuleFactory}
 */
public interface PrefixModuleFactory extends ModuleFactory
{
    /**
     * @return the prefix the module factory expects to be matched to
     */
    String getPrefix();
}
