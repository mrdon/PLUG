package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.web.model.WebIcon;
import com.atlassian.plugin.web.model.WebLink;

/**
 * A web-item plugin adds extra links to a particular section.
 *
 * @see WebSectionModuleDescriptor
 */
public interface WebItemModuleDescriptor<T> extends WebLinkFragmentModuleDescriptor<T>
{
    String getSection();

    WebLink getLink();

    WebIcon getIcon();

    /**
     * Returns the item style as a "class" String consisting of one or more style classes.
     * The default value returned should be an empty String rather than null.
     * <p>
     * Where possible, use of this method is preferred over <code>getIcon</code> as it
     * allows more flexibility for CSS-based web element styling and class-based
     * JavaScript behaviour.
     *
     * @return space-separated list of style classes
     */
    String getStyleClass();
}
