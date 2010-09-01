package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.conditions.ConditionLoadingException;
import com.atlassian.plugin.web.conditions.AlwaysDisplayCondition;
import com.atlassian.plugin.web.conditions.NeverDisplayCondition;
import com.atlassian.plugin.Plugin;

import java.util.List;
import java.util.Map;

public class MockWebFragmentHelper implements WebFragmentHelper
{
    public Condition loadCondition(String className, Plugin plugin) throws ConditionLoadingException
    {
        if (className.indexOf("AlwaysDisplayCondition") != -1)
        {
            return new AlwaysDisplayCondition();
        }
        else
        {
            return new NeverDisplayCondition();
        }
    }

    public ContextProvider loadContextProvider(String className, Plugin plugin) throws ConditionLoadingException
    {
        if (className.endsWith("TestContextProvider"))
        {
            return new TestContextProvider();
        }
        return null;
    }

    public String getI18nValue(String key, List arguments, Map context)
    {
        return null;
    }

    public String renderVelocityFragment(String fragment, Map context)
    {
        return null;
    }
}
