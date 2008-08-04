package com.atlassian.plugin.web.conditions;

import com.atlassian.plugin.web.Condition;

import java.util.Iterator;
import java.util.Map;

public class AndCompositeCondition extends AbstractCompositeCondition
{
    public boolean shouldDisplay(Map<String,Object> context)
    {
        for (Condition condition : conditions)
        {
            if (!condition.shouldDisplay(context))
                return false;
        }

        return true;
    }
}
