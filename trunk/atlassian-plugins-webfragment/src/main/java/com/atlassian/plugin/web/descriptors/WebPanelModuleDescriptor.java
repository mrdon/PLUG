package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.model.WebPanel;

/**
 * <p>
 * The web panel module declares a single web panel in atlassian-plugin.xml. Its
 * XML element contains a location string that should match existing locations
 * in the host application where web panels can be embedded.
 * </p>
 * <p>
 * The descriptor specifies a resource or class that renders HTML given a context map,
 * and may specify a {@link ContextProvider} that augments the context with custom
 * properties.
 * </p>
 *
 * @since   2.6.0
 */
public interface WebPanelModuleDescriptor extends WebFragmentModuleDescriptor<WebPanel>, WeightedDescriptor
{
    /**
     * Returns the location in the host application where the web panel should be embedded.
     */
    String getLocation();
}
