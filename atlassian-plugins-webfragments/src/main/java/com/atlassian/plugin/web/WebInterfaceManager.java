package com.atlassian.plugin.web;

import java.util.List;
import java.util.Map;

/**
 * A simple manager to provide sections of the web interface through plugins.
 */
public interface WebInterfaceManager
{
    /**
     * @return True if there are any sections for the given location.
     */
    boolean hasSectionsForLocation(String location);

    /**
     * @return A list of all WebSectionModuleDescriptors for the given location.
     */
    List getSections(String location);

    /**
     * @return A list of all AbstractWebFragmentModuleDescriptor <i>viewable in a given context</i> in the given location.
     */
    List getDisplayableSections(String location, Map context);

    /**
     * @return A list of all WebItemModuleDescriptors for the given section.
     */
    List getItems(String section);

    /**
     * @return A list of all AbstractWebFragmentModuleDescriptor <i>viewable in a given context</i> in the given section.
     */
    List getDisplayableItems(String section, Map context);

    /**
     * Refresh the contents of the web interface manager.
     */
    void refresh();

    /**
     * @return The web fragment helper for this implementation.
     */
    WebFragmentHelper getWebFragmentHelper();
}
