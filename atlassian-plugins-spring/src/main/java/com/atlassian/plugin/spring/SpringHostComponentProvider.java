package com.atlassian.plugin.spring;

import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * Host component provider that scans the bean factory for all beans that implement @AvailableToPlugins and registers
 * them.
 */
@Component("hostComponentProvider")
public class SpringHostComponentProvider implements HostComponentProvider, BeanFactoryAware
{
    private BeanFactory beanFactory = null;

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = beanFactory;
    }

    public void provide(ComponentRegistrar registrar)
    {
        ListableBeanFactory lbf;
        if (beanFactory instanceof ListableBeanFactory) {
            lbf = (ListableBeanFactory) beanFactory;
            String[] names = lbf.getBeanDefinitionNames();

            for (String name : names)
            {
                if (lbf.isSingleton(name))
                {
                    try
                    {
                        Object bean = lbf.getBean(name);
                        AvailableToPlugins annotation = bean.getClass().getAnnotation(AvailableToPlugins.class);
                        Class[] ifs;
                        if (annotation != null) {
                            if (annotation.value() != Void.class) {
                                ifs = new Class[] { annotation.value() };
                            } else {
                                ifs = bean.getClass().getInterfaces();
                            }
                            registrar.register(ifs).forInstance(bean).withName(name);
                        }
                    } catch (BeanIsAbstractException ex)
                    {
                        // skipping abstract beans (is there a better way to check for this?)
                    }
                }
            }
        }
    }
}
