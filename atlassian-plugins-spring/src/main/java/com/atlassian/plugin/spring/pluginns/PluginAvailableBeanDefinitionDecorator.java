package com.atlassian.plugin.spring.pluginns;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.lang.IllegalStateException;
import static com.atlassian.plugin.spring.pluginns.SpringXmlHostComponentProvider.HOST_COMPONENT_PROVIDER;

/**
 * Processes an "available" attribute in the plugin namespace.  Also handles registering the {@link SpringXmlHostComponentProvider}.
 *
 * In the case of hierarchical contexts we will put the host
 * component provider in the lowest possible context.
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
    public BeanDefinitionHolder decorate(
            Node source, BeanDefinitionHolder holder, ParserContext ctx)
    {

        String isAvailable = ((Attr) source).getValue();
        if (Boolean.parseBoolean(isAvailable))
        {
            BeanDefinition providerDef = registerHostComponent(ctx);
            List<String> registrations = (List<String>) providerDef.getPropertyValues().getPropertyValue("registrations").getValue();
            registrations.add(holder.getBeanName());
        }
        return holder;

    }

    /**
     * Registered a host component provider, if none already exist
     * @param ctx The parser context
     * @return The discovered host component provider definition
     */
    private BeanDefinition registerHostComponent(ParserContext ctx)
    {
        BeanDefinition providerDef;

        BeanDefinitionRegistry registry = ctx.getRegistry();

        if (!registry.containsBeanDefinition(HOST_COMPONENT_PROVIDER))
        {
            BeanDefinitionBuilder providerDefBuilder = BeanDefinitionBuilder.rootBeanDefinition(SpringXmlHostComponentProvider.class);
            providerDefBuilder.addPropertyValue("registrations", new ArrayList());
            providerDef = providerDefBuilder.getBeanDefinition();
            ctx.getRegistry().registerBeanDefinition(HOST_COMPONENT_PROVIDER, providerDef);
        }
        else
            providerDef = registry.getBeanDefinition(HOST_COMPONENT_PROVIDER);


        if (providerDef == null)
        {
            throw new IllegalStateException("Host component provider not found or created. This should never happen.");
        }
        
        return providerDef;
    }
}
