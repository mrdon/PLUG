package com.atlassian.plugin.refimpl;

import com.atlassian.sal.spi.HostContextAccessor;

import java.util.Map;

public class RiHostContextAccessor implements HostContextAccessor
{
    public <T> Map<String, T> getComponentsOfType(Class<T> tClass)
    {
        return null;
    }

    public Object doInTransaction(HostTransactionCallback hostTransactionCallback)
    {
        return null;
    }
}
