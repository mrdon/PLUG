package com.atlassian.plugin.web.model;

import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.WebFragmentHelper;

import java.util.Map;
import java.util.HashMap;

/**
 * Represents web items that can be rendered using velocity, and inject its
 * own context using the {@link ContextProvider}
 */
public abstract class AbstractWebItem
{
    protected WebFragmentHelper webFragmentHelper;
    private ContextProvider contextProvider;

    protected AbstractWebItem(WebFragmentHelper webFragmentHelper, ContextProvider contextProvider)
    {
        this.webFragmentHelper = webFragmentHelper;
        this.contextProvider = contextProvider;
    }

    public Map getContextMap()
    {
        if (contextProvider != null)
        {
            return contextProvider.getContextMap();
        }
        return new HashMap();
    }
}
