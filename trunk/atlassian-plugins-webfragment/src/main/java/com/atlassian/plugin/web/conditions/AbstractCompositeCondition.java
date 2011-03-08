package com.atlassian.plugin.web.conditions;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractCompositeCondition implements Condition
{
    protected List<Condition> conditions = new ArrayList<Condition>();

    public AbstractCompositeCondition()
    {
    }

    public void addCondition(Condition condition)
    {
        this.conditions.add(condition);
    }

    public void init(Map<String,String> params) throws PluginParseException
    {
    }

    public abstract boolean shouldDisplay(Map<String,Object> context);
}
