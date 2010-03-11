package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.web.model.WebLabel;

/**
 * A convenience interface for web fragment descriptors that contain links
 * (currently web items and web sections).
 */
public interface WebLinkFragmentModuleDescriptor extends WebFragmentModuleDescriptor<Void>
{
    WebLabel getWebLabel();

    WebLabel getTooltip();
}
