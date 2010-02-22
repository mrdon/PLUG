package com.atlassian.plugin;

import java.util.Collections;
import java.util.List;

/**
 * TODO: Document this class / interface here
 */
public class DefaultModuleCreator implements ModuleCreator
{
    private final List<BeanResolver> resolvers;
    private Object container;

    public DefaultModuleCreator(List<BeanResolver> resolvers)
    {
        this.resolvers = resolvers;
    }

    public DefaultModuleCreator()
    {
        resolvers = Collections.emptyList();
    }

    public Object create(final String className, final Plugin plugin)
    {
        String prefix = "class";
        final int prefixIndex = className.indexOf(":");
        if (prefixIndex != -1)
        {
            prefix = className.substring(prefixIndex);
        }

        for (BeanResolver resolver : resolvers)
        {
            if (resolver.supportsPrefix(prefix))
            {
                return resolver.resolveNameToObject(className);
            }
        }
        return null;
    }

    public void autowire(final Object object, final Plugin plugin)
    {
        for (BeanResolver resolver : resolvers)
        {
            if (resolver.supportsPrefix("class"))
            {
                resolver.autowire(object, plugin);
            }
        }

    }

    public void setPluginContainer(final Object container)
    {
        this.container = container;
    }
}
