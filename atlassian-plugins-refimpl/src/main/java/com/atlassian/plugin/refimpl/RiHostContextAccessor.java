package com.atlassian.plugin.refimpl;

import com.atlassian.sal.spi.HostContextAccessor;

import java.util.Map;
import java.util.Collections;

public class RiHostContextAccessor implements HostContextAccessor
{
    private Map<Class, Object> container;

    public RiHostContextAccessor(Map<Class, Object> container)
    {
        this.container = container;
    }

    public <T> Map<String, T> getComponentsOfType(Class<T> tClass)
    {
        return (Map<String, T>) Collections.singletonMap("component", container.get(tClass));
    }

    public Object doInTransaction(HostTransactionCallback hostTransactionCallback)
    {
        return hostTransactionCallback.doInTransaction();
    }
}
