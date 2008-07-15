package com.atlassian.plugin.web;

import com.atlassian.plugin.PluginParseException;

import java.util.Map;

/**
 * Decides whether a web section or web item should be displayed
 */
public interface Condition
{
    /**
     * Called after creation and autowiring.
     *
     * @param params The optional map of parameters specified in XML.
     */
    void init(Map params) throws PluginParseException;

    /**
     * Determine whether the web fragment should be displayed
     *
     * @return true if the user should see the fragment, false otherwise
     */
    boolean shouldDisplay(Map context);
}
