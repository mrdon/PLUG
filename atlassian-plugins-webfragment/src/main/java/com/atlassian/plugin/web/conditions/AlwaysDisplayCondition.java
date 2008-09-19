package com.atlassian.plugin.web.conditions;

import com.atlassian.plugin.web.Condition;

import java.util.Map;

/**
 * Always show a web link. Not really useful for anything except testing
 */
public class AlwaysDisplayCondition implements Condition
{
    public void init(Map<String,String> params)
    {
    }

    public boolean shouldDisplay(Map<String,Object> context)
    {
        return true;
    }
}
