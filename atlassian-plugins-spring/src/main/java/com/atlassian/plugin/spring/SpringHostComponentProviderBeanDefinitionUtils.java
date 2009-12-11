package com.atlassian.plugin.spring;

import com.atlassian.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpringHostComponentProviderBeanDefinitionUtils
{
    public static final String HOST_COMPONENT_PROVIDER = "hostComponentProvider";

    private static final String BEAN_NAMES = "beanNames";
    private static final String BEAN_INTERFACES = "beanInterfaces";
    private static final String BEAN_CONTEXT_CLASS_LOADER_STRATEGIES = "beanContextClassLoaderStrategies";

    public static BeanDefinition getBeanDefinition(BeanDefinitionRegistry registry)
    {
        if (!registry.containsBeanDefinition(HOST_COMPONENT_PROVIDER))
        {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SpringHostComponentProviderFactoryBean.class);
            builder.addPropertyValue(BEAN_NAMES, new ArrayList<String>());
            builder.addPropertyValue(BEAN_INTERFACES, new HashMap<String, List<String>>());
            builder.addPropertyValue(BEAN_CONTEXT_CLASS_LOADER_STRATEGIES, new HashMap<String, ContextClassLoaderStrategy>());
            builder.addPropertyValue("useAnnotation", false); // by default we don't want to scan for annotation

            registry.registerBeanDefinition(HOST_COMPONENT_PROVIDER, builder.getBeanDefinition());
        }

        final BeanDefinition beanDef = registry.getBeanDefinition(HOST_COMPONENT_PROVIDER);
        if (beanDef == null)
        {
            throw new IllegalStateException("Host component provider not found nor created. This should never happen.");
        }
        return beanDef;
    }

    public static void addBeanName(BeanDefinitionRegistry registry, String beanName)
    {
        getBeanNames(registry).add(beanName);
    }

    public static void addBeanInterface(BeanDefinitionRegistry registry, String beanName, String ifce)
    {
        addBeanInterfaces(registry, beanName, Collections.singleton(ifce));
    }

    public static void addBeanInterfaces(BeanDefinitionRegistry registry, String beanName, Collection<String> ifces)
    {
        final Map<String, List<String>> beanInterfaces = getBeanInterfaces(registry);

        List<String> interfaces = beanInterfaces.get(beanName);
        if (interfaces == null)
        {
            interfaces = new ArrayList<String>();
            beanInterfaces.put(beanName, interfaces);
        }
        interfaces.addAll(ifces);
    }

    public static void addContextClassLoaderStrategy(BeanDefinitionRegistry registry, String beanName, ContextClassLoaderStrategy strategy)
    {
        getBeanContextClassLoaderStrategies(registry).put(beanName, strategy);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ContextClassLoaderStrategy> getBeanContextClassLoaderStrategies(BeanDefinitionRegistry registry)
    {
        return (Map<String, ContextClassLoaderStrategy>) getPropertyValue(registry, BEAN_CONTEXT_CLASS_LOADER_STRATEGIES);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> getBeanInterfaces(BeanDefinitionRegistry registry)
    {
        return (Map<String, List<String>>) getPropertyValue(registry, BEAN_INTERFACES);
    }

    @SuppressWarnings("unchecked")
    private static List<String> getBeanNames(BeanDefinitionRegistry registry)
    {
        return (List<String>) getPropertyValue(registry, BEAN_NAMES);
    }

    private static Object getPropertyValue(BeanDefinitionRegistry registry, String propertyName)
    {
        return getBeanDefinition(registry).getPropertyValues().getPropertyValue(propertyName).getValue();
    }
}
