package com.atlassian.plugin.web.conditions;

import com.atlassian.plugin.web.Condition;

import java.util.Map;

/**
 * Always hide a web link. Not really useful for anything except testing
 */
public class NeverDisplayCondition implements Condition
{
    public void init(Map params)
    {
    }

    public boolean shouldDisplay(Map context)
    {
        return false;
    }
}
