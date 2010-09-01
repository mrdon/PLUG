package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;

import java.util.Map;

public class TestContextProvider implements ContextProvider
{
    public void init(Map<String, String> params) throws PluginParseException
    {
    }

    public Map<String, Object> getContextMap(Map<String, Object> context)
    {
        return context;
    }
}
