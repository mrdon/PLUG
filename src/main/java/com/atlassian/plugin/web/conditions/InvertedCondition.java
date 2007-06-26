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

    public void init(Map params) throws PluginParseException
    {
    }

    public boolean shouldDisplay(Map context)
    {
        return !wrappedCondition.shouldDisplay(context);
    }
}
