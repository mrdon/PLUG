package com.atlassian.plugin;

import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.elements.ResourceDescriptor;

import java.util.List;

public interface Resourced
{
    List getResourceDescriptors();

    List getResourceDescriptors(String type);

    ResourceLocation getResourceLocation(String type, String name);

    /**
     * @deprecated
     */
    ResourceDescriptor getResourceDescriptor(String type, String name);
}
