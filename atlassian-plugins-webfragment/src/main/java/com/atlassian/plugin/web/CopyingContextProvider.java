package com.atlassian.plugin.web;

import com.atlassian.plugin.PluginParseException;

import java.util.HashMap;
import java.util.Map;

/**
 * Copies the given context before passing it to a delegate ContextProvider implementation.
 */
public class CopyingContextProvider implements ContextProvider
{
    private final ContextProvider delegate;

    public CopyingContextProvider(ContextProvider delegate)
    {
        this.delegate = delegate;
    }

    public void init(Map<String, String> params) throws PluginParseException
    {
    }

    public Map<String, Object> getContextMap(Map<String, Object> context)
    {
        Map<String,Object> contextCopy = new HashMap<String,Object>(context);
        return delegate.getContextMap(contextCopy);
    }

    public ContextProvider getDelegate()
    {
        return delegate;
    }
}
