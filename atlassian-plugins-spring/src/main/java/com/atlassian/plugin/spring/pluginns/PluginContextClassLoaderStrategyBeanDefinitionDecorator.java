package com.atlassian.plugin.spring.pluginns;

import com.atlassian.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import com.atlassian.plugin.spring.PluginBeanDefinitionRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * Processes an "contextClassLoader" strategy attribute in the plugin namespace.
 * Also handles registering the {@link com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider} through
 * the {@link com.atlassian.plugin.spring.SpringHostComponentProviderFactoryBean}.
 *
 * In the case of hierarchical contexts we will put the host component provider in the lowest possible context.
 */
public class PluginContextClassLoaderStrategyBeanDefinitionDecorator implements BeanDefinitionDecorator
{
    private final static Log log = LogFactory.getLog(PluginContextClassLoaderStrategyBeanDefinitionDecorator.class);

    /**
     * Called when the Spring parser encounters an "available" attribute.
     * @param source The attribute
     * @param holder The containing bean definition
     * @param ctx The parser context
     * @return The containing bean definition
     */
    public BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder holder, ParserContext ctx)
    {
        final String contextClassLoaderStrategy = ((Attr) source).getValue();
        if (contextClassLoaderStrategy != null)
        {
            new PluginBeanDefinitionRegistry(ctx.getRegistry()).addContextClassLoaderStrategy(holder.getBeanName(), getContextClassLoaderStrategy(contextClassLoaderStrategy));
        }
        return holder;

    }

    private ContextClassLoaderStrategy getContextClassLoaderStrategy(String contextClassLoaderStrategy)
    {
        try
        {
            return ContextClassLoaderStrategy.valueOf(contextClassLoaderStrategy);
        }
        catch (IllegalArgumentException e)
        {
            log.warn("Cannot parse '" + contextClassLoaderStrategy + "' to a valid context class loader strategy, will use default '" + ContextClassLoaderStrategy.USE_HOST + "'");
            return ContextClassLoaderStrategy.USE_HOST;
        }
    }
}