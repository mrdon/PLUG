package com.atlassian.plugin.osgi.spring;

import com.atlassian.plugin.module.ContainerAccessor;
import com.atlassian.plugin.AutowireCapablePlugin;

public interface SpringContainerAccessor extends ContainerAccessor
{
    Object getBean(String id);

    @Deprecated
    void autowireBean(Object instance, AutowireCapablePlugin.AutowireStrategy strategy);
}
