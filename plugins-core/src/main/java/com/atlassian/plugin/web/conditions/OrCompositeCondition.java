package com.atlassian.plugin.web.conditions;

import com.atlassian.plugin.web.Condition;

import java.util.Iterator;
import java.util.Map;

public class OrCompositeCondition extends AbstractCompositeCondition
{
    public boolean shouldDisplay(Map context)
    {
        for (Iterator it = conditions.iterator(); it.hasNext();)
        {
            Condition condition = (Condition) it.next();
            if (condition.shouldDisplay(context))
                return true;
        }

        return false;
    }
}
