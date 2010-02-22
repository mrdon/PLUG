package com.atlassian.plugin;

/**
 * TODO: Document this class / interface here
 */
public interface BeanResolver
{
    boolean supportsPrefix(String prefix);

    Object resolveNameToObject(String name);

    void autowire (Object object, Plugin plugin);

    /**
     * @param container the plugin container (spring context)
     */
    void setPluginContainer(Object container);

}
