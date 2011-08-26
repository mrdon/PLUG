package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.google.common.base.Function;

class TransformDescriptorToKey implements Function<ModuleDescriptor<?>, String>
{
    public String apply(final ModuleDescriptor<?> resource)
    {
        return resource.getCompleteKey();
    }
}
