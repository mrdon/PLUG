package com.atlassian.labs.plugins3.spring;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.labs.plugins3.api.annotation.HostComponent;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;

/**
 *
 */
public class HostComponentBeanFactoryPostProcessor implements BeanFactoryPostProcessor
{
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        for (String name : beanFactory.getBeanDefinitionNames())
        {
            BeanDefinition def = beanFactory.getBeanDefinition(name);
            try
            {
                Class beanClass = beanFactory.getBeanClassLoader().loadClass(def.getBeanClassName());
                for (Constructor ctr : beanClass.getConstructors())
                {
                    if (ctr.getAnnotation(Inject.class) != null)
                    {
                        for (int x=0; x< ctr.getParameterTypes().length; x++)
                        {
                            Class paramType = ctr.getParameterTypes()[x];
                            for (Annotation ann : ctr.getParameterAnnotations()[x])
                            {
                                if (ann.annotationType() == HostComponent.class)
                                {
                                    registerHostComponentBean(registry, paramType, (HostComponent)ann);
                                }
                            }
                        }
                    }
                }
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private void registerHostComponentBean(BeanDefinitionRegistry registry, Class paramType, HostComponent ann)
    {
        String hostBeanName = ann.value();
        String filter = "(&(" + ComponentRegistrar.HOST_COMPONENT_FLAG + "=true)(objectClass=" + paramType.getName() + "))";
        if ("".equals(hostBeanName))
        {
            hostBeanName = "host-" + paramType.getSimpleName();
        }
        else
        {
            filter = "(&(bean-name=" + hostBeanName + ")" + filter + ")";
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(HostComponentFactoryBean.class);
        builder.setLazyInit(true);
        builder.addPropertyValue("filter", filter);
        builder.addPropertyValue("interfaces", asList(paramType.getName()));
        registry.registerBeanDefinition(hostBeanName, builder.getBeanDefinition());
    }
}
