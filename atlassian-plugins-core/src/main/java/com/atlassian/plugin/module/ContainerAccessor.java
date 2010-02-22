package com.atlassian.plugin.module;

public interface ContainerAccessor
{
    <T> T createBean(Class<T> clazz);
}
