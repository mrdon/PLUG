package com.atlassian.plugin.webresource.transformer;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;

/**
 * Defines a module descriptor for a {@link WebResourceTransformer}.
 *
 * @since 2.5.0
 */
public class WebResourceTransformerModuleDescriptor extends AbstractModuleDescriptor<WebResourceTransformer> 
{
    private final ModuleFactory moduleFactory;

    public WebResourceTransformerModuleDescriptor(ModuleFactory moduleFactory) {
        this.moduleFactory = moduleFactory;
    }

    @Override
    public WebResourceTransformer getModule() {
        return moduleFactory.createModule(moduleClassName, this);
    }

}
