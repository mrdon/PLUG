package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.web.ContextProvider;

/**
 * Makes a plugin module aware of its Velocity context. Web modules should
 * implement this interface if they want to modify the Velocity context used to
 * render their content.
 */
public interface ContextAware
{
    /**
     * Returns the ContextProvider that augments the context used to render a
     * web module.
     */
    ContextProvider getContextProvider();

}
