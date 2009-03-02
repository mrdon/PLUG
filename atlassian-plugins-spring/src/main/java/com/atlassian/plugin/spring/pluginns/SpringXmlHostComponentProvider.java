package com.atlassian.plugin.spring.pluginns;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.BeansException;
import org.apache.commons.lang.ClassUtils;

/**
 * Host component provider that uses <code>plugin:available="true"</code> attributes in Spring XML bean configuration
 * elements to determine which host components to provide to plugins.
 *
 * <p>When searching for interfaces, all spring framework and java.* interfaces are ignored.</p>
 */
public class SpringXmlHostComponentProvider implements HostComponentProvider, BeanFactoryAware
{
    public static final String HOST_COMPONENT_PROVIDER = "hostComponentProvider";
    private BeanFactory beanFactory;
    private List<String> beans;
    private Map<String,Class[]> interfaces = Collections.emptyMap();

    public void setRegistrations(List<String> names)
    {
        this.beans = names;
    }

    public void setInterfaces(Map<String,Class[]> interfaces)
    {
        this.interfaces = interfaces;
    }

    public void provide(ComponentRegistrar registrar)
    {
        for (String name : beans)
        {
            Object bean = beanFactory.getBean(name);
            Class[] beanInterfaces = interfaces.get(name);
            if (beanInterfaces == null)
            {
                beanInterfaces = findInterfaces(bean.getClass());
            }
            registrar.register(beanInterfaces).forInstance(bean)
                    .withName(name);
        }
        if (beanFactory instanceof HierarchicalBeanFactory)
        {
            HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) beanFactory;
            if (hbf.getParentBeanFactory() != null)
            {
                HostComponentProvider provider = (HostComponentProvider) hbf.getParentBeanFactory().getBean(HOST_COMPONENT_PROVIDER);
                if (provider != null)
                    provider.provide(registrar);
            }
        }
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = beanFactory;
    }

    Class[] findInterfaces(Class cls)
    {
        List<Class> validInterfaces = new ArrayList<Class>();
        List<Class> allInterfaces = ClassUtils.getAllInterfaces(cls);
        for (Class inf : allInterfaces)
        {
            if (!inf.getName().startsWith("org.springframework") && !inf.getName().startsWith("java."))
                validInterfaces.add(inf);
        }
        return validInterfaces.toArray(new Class[0]);
    }
}
