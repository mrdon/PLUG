package com.atlassian.plugin.web.descriptors;

/**
 * A web-section plugin adds extra sections to a particular location.
 */
public interface WebSectionModuleDescriptor<T> extends WebLinkFragmentModuleDescriptor<T>
{
    String getLocation();
}
