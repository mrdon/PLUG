package com.atlassian.plugin.spring.pluginns;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Handler for the "plugin" namespace
 */
public class PluginNamespaceHandler extends NamespaceHandlerSupport
{
    /**
     * Registeres the "available" attribute for beans
     */
    public void init()
    {
        super.registerBeanDefinitionDecoratorForAttribute("available",
                new PluginAvailableBeanDefinitionDecorator());
    }

}
