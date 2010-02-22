package com.atlassian.plugin.module;

import com.atlassian.plugin.module.ModuleCreator;
import com.atlassian.plugin.module.ModulePrefixProvider;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;

import java.util.Collections;
import java.util.List;

/**
 * TODO: Document this class / interface here
 */
public class DefaultModuleCreator implements ModuleCreator
{
    private final List<ModulePrefixProvider> providers;

    public DefaultModuleCreator(List<ModulePrefixProvider> providers)
    {
        this.providers = providers;
    }

    public Object create(String className, final ModuleDescriptor moduleDescriptor)
    {
        String prefix = "class";
        final int prefixIndex = className.indexOf(":");
        if (prefixIndex != -1)
        {
            className = className.substring(prefixIndex + 1);
        }

        Object result = null;
        for (ModulePrefixProvider prefixProvider : providers)
        {
            if (prefixProvider.supportsPrefix(prefix))
            {
                result = prefixProvider.create(className, moduleDescriptor);
                break;
            }
        }
        if (result != null)
        {
            return result;
        }
        else
        {
            throw new PluginParseException("Unable to create module instance from '" + className + "'");
        }
    }

}
