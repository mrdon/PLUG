package com.atlassian.plugin.module;

/**
 * The ContainerAccessor allows to create a bean in a plugin container (e.g. spring).
 * 
 * @since 2.5.0
 */
public interface ContainerAccessor
{
    /**
     * Will ask the container to instantiate a bean of the given class and does inject all constructor defined dependencies.
     * Currently we have only spring as a container that will autowire this bean.
     *
     * @param clazz the Class to instantiate. Cannot be null.
     *
     * @return an instantiated bean.
     */
    <T> T createBean(Class<T> clazz);
}
