package com.atlassian.plugin.web.conditions;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;

public class CompositeCondition implements Condition
{
    private List conditions = new ArrayList();

    public CompositeCondition()
    {
    }

    public void addCondition(Condition condition)
    {
        this.conditions.add(condition);
    }

    public void init(Map params) throws PluginParseException
    {
    }

    public boolean shouldDisplay(Map context)
    {
        for (Iterator it = conditions.iterator(); it.hasNext();)
        {
            Condition condition = (Condition) it.next();
            if (!condition.shouldDisplay(context))
                return false;
        }

        return true;
    }
}
