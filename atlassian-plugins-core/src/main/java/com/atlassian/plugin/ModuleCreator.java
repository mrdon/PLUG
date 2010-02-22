package com.atlassian.plugin;

/**
 * TODO: Document this class / interface here
 */
public interface ModuleCreator
{
    Object create (String className,Plugin plugin);

    void autowire(Object object, Plugin plugin);

    /**
     * @param container the plugin container (spring context)
     */
    void setPluginContainer(Object container);

}
