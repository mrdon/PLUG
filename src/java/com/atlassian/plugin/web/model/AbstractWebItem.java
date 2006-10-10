package com.atlassian.plugin.web.model;

import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.descriptors.AbstractWebFragmentModuleDescriptor;

import java.util.*;

/**
 * Represents web items that can be rendered using velocity, and inject its
 * own context using the {@link ContextProvider}
 */
public abstract class AbstractWebItem
{
    private WebFragmentHelper webFragmentHelper;
    private ContextProvider contextProvider;
    private final AbstractWebFragmentModuleDescriptor descriptor;

    protected AbstractWebItem(WebFragmentHelper webFragmentHelper, ContextProvider contextProvider, AbstractWebFragmentModuleDescriptor descriptor)
    {
        this.webFragmentHelper = webFragmentHelper;
        this.contextProvider = contextProvider;
        this.descriptor = descriptor;
    }

    public Map getContextMap()
    {
        if (contextProvider != null)
        {
            return contextProvider.getContextMap();
        }
        return new HashMap();
    }

    public WebFragmentHelper getWebFragmentHelper()
    {
        return webFragmentHelper;
    }

    public AbstractWebFragmentModuleDescriptor getDescriptor()
    {
        return descriptor;
    }
}
