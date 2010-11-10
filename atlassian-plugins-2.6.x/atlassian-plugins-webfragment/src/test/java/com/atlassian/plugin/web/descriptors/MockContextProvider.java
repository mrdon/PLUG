package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;

import java.util.Map;

public class MockContextProvider implements ContextProvider
{
    static final String KEY_ADDED_TO_CONTEXT = "addedToContext";
    static final String DATA_ADDED_TO_CONTEXT = "data added to context";

    public void init(Map<String, String> params) throws PluginParseException
    {
    }

    public Map<String, Object> getContextMap(Map<String, Object> context)
    {
        context.put(KEY_ADDED_TO_CONTEXT, DATA_ADDED_TO_CONTEXT);
        return context;
    }
}
