package com.atlassian.plugin.web.model;

import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.descriptors.WebFragmentModuleDescriptor;

import java.util.*;

/**
 * Represents web items that can be rendered using velocity, and inject its
 * own context using the {@link ContextProvider}
 */
public abstract class AbstractWebItem
{
    private WebFragmentHelper webFragmentHelper;
    private ContextProvider contextProvider;
    private final WebFragmentModuleDescriptor descriptor;

    protected AbstractWebItem(WebFragmentHelper webFragmentHelper, ContextProvider contextProvider, WebFragmentModuleDescriptor descriptor)
    {
        this.webFragmentHelper = webFragmentHelper;
        this.contextProvider = contextProvider;
        this.descriptor = descriptor;
    }

    public Map<String,Object> getContextMap(Map<String,Object> context)
    {
        if (contextProvider != null)
        {
            return contextProvider.getContextMap(context);
        }
        return Collections.EMPTY_MAP;
    }

    public WebFragmentHelper getWebFragmentHelper()
    {
        return webFragmentHelper;
    }

    public WebFragmentModuleDescriptor getDescriptor()
    {
        return descriptor;
    }
}
