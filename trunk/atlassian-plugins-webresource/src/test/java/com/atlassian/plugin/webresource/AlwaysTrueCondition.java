package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

public class AlwaysTrueCondition implements Condition
{
    public void init(Map<String, String> params) throws PluginParseException
    {
    }

    public boolean shouldDisplay(Map<String, Object> context)
    {
        return true;
    }
}
