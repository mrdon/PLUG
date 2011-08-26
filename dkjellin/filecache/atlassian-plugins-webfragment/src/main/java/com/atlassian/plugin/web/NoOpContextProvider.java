package com.atlassian.plugin.web;

import com.atlassian.plugin.PluginParseException;

import java.util.Map;

/**
 * A {@link ContextProvider} implementation that just passes the given context back. Used when no Context Provider has
 * been specified in the ModuleDescriptor.
 */
public class NoOpContextProvider implements ContextProvider
{
    public void init(Map<String, String> params) throws PluginParseException
    {
    }

    public Map<String, Object> getContextMap(Map<String, Object> context)
    {
        return context;
    }
}
