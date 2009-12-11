package com.atlassian.plugin.spring.pluginns;

import com.atlassian.plugin.spring.SpringHostComponentProviderBeanDefinitionUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * Processes an "available" attribute in the plugin namespace.
 * Also handles registering the {@link com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider} through
 * the {@link com.atlassian.plugin.spring.SpringHostComponentProviderFactoryBean}.
 *
 * In the case of hierarchical contexts we will put the host component provider in the lowest possible context.
 */
public class PluginAvailableBeanDefinitionDecorator implements BeanDefinitionDecorator
{
    /**
     * Called when the Spring parser encounters an "available" attribute.
     * @param source The attribute
     * @param holder The containing bean definition
     * @param ctx The parser context
     * @return The containing bean definition
     */
    public BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder holder, ParserContext ctx)
    {
        final String isAvailable = ((Attr) source).getValue();
        if (Boolean.parseBoolean(isAvailable))
        {
            SpringHostComponentProviderBeanDefinitionUtils.addBeanName(ctx.getRegistry(), holder.getBeanName());
        }
        return holder;
    }
}
