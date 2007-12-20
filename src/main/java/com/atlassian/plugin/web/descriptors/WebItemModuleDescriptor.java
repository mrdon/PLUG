package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.web.model.WebIcon;
import com.atlassian.plugin.web.model.WebLink;

/**
 * A web-item plugin adds extra links to a particular section.
 *
 * @see WebSectionModuleDescriptor
 */
public interface WebItemModuleDescriptor extends WebFragmentModuleDescriptor
{
    String getSection();

    WebLink getLink();

    WebIcon getIcon();

    String getStyleName();
}
