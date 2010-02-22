package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;

/**
 * TODO: Document this class / interface here
 */
public interface ModuleCreator
{
    /**
     *
     * @param className
     * @param moduleDescriptor
     * @return
     * @throws PluginParseException If the className couldn't be resolved
     */
    Object create (String className, ModuleDescriptor moduleDescriptor) throws PluginParseException;


}
