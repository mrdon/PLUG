package com.atlassian.plugin.spring.pluginns;

import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import org.apache.commons.lang.ClassUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Host component provider that uses <code>plugin:available="true"</code> attributes in Spring XML bean configuration
 * elements to determine which host components to provide to plugins.
 *
 * <p>When searching for interfaces, all spring framework and java.* interfaces are ignored.</p>
 */
public class SpringXmlHostComponentProvider implements HostComponentProvider, BeanFactoryAware
{
    private static final Logger log = Logger.getLogger(SpringXmlHostComponentProvider.class);

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
            if (beanFactory.isSingleton(name))
            {
                Class[] beanInterfaces = interfaces.get(name);
                if (beanInterfaces == null)
                {
                    beanInterfaces = findInterfaces(bean.getClass());
                }
                registrar.register(beanInterfaces).forInstance(bean)
                        .withName(name);
            }
            else
            {
                log.warn("Cannot register bean " + name + " as it's scope is not singleton");
            }
        }
        if (beanFactory instanceof HierarchicalBeanFactory)
        {
            HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) beanFactory;
            final BeanFactory parentBeanFactory = hbf.getParentBeanFactory();
            if (parentBeanFactory != null)
            {
                try
                {
                    HostComponentProvider provider = (HostComponentProvider) parentBeanFactory.getBean(HOST_COMPONENT_PROVIDER);
                    if (provider != null)
                        provider.provide(registrar);
                }
                catch (NoSuchBeanDefinitionException e)
                {
                    log.info("Unable to find '" + HOST_COMPONENT_PROVIDER + "' in the parent bean factory " + parentBeanFactory);
                }
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
