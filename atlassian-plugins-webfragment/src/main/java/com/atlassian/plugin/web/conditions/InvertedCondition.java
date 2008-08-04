package com.atlassian.plugin.web.conditions;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

public class InvertedCondition implements Condition
{
    private Condition wrappedCondition;

    public InvertedCondition(Condition wrappedCondition)
    {
        this.wrappedCondition = wrappedCondition;
    }

    public void init(Map<String,String> params) throws PluginParseException
    {
    }

    public boolean shouldDisplay(Map<String,Object> context)
    {
        return !wrappedCondition.shouldDisplay(context);
    }
}
