package com.atlassian.plugin.webresource;

/**
 * Represents the types of web resources. Used as inputs to {@code WebResourceManager} methods to include/get
 * resources of a certain type.
 *
 * @since 2.4
 */
public enum WebResourceType
{
    /**
     * CSS resources.
     */
    CSS,
    /**
     * Javascript resources
     */
    JAVASCRIPT,
    /**
     * All supported web resources.
     */
    ALL;
}
