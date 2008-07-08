package com.atlassian.plugin.spring.pluginns;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;

import java.util.List;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.BeansException;

/**
 * Host component provider that uses <code>plugin:available="true"</code> attributes in Spring XML bean configuration
 * elements to determine which host components to provide to plugins.
 */
public class SpringXmlHostComponentProvider implements HostComponentProvider, BeanFactoryAware
{
    private BeanFactory beanFactory;
    private List<String> beans;

    public void setRegistrations(List<String> names)
    {
        this.beans = names;
    }
    public void provide(ComponentRegistrar registrar)
    {
        for (String name : beans)
        {
            Object bean = beanFactory.getBean(name);
            registrar.register(bean.getClass().getInterfaces()).forInstance(bean)
                    .withName(name);
        }
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = beanFactory;
    }
}
