package com.atlassian.plugin.web;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.web.conditions.ConditionLoadingException;

import java.util.List;
import java.util.Map;

public interface WebFragmentHelper
{
    Condition loadCondition(String className, Plugin plugin) throws ConditionLoadingException;

    ContextProvider loadContextProvider(String className, Plugin plugin) throws ConditionLoadingException;

    String getI18nValue(String key, List arguments, Map context);

    String renderVelocityFragment(String fragment, Map context);
}
