package com.atlassian.plugin;

import com.atlassian.plugin.elements.ResourceDescriptor;

import java.util.List;

public interface Resourced
{
    List getResourceDescriptors();

    List getResourceDescriptors(String type);

    ResourceDescriptor getResourceDescriptor(String type, String name);
}
