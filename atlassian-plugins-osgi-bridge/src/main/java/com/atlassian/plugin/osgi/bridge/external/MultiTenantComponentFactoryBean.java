package com.atlassian.plugin.osgi.bridge.external;

import com.atlassian.multitenant.MultiTenantComponentFactory;
import com.atlassian.multitenant.MultiTenantComponentMap;
import com.atlassian.multitenant.MultiTenantContext;
import com.atlassian.multitenant.MultiTenantCreator;
import com.atlassian.multitenant.MultiTenantDestroyer;
import com.atlassian.multitenant.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Spring FactoryBean, that returns a proxy to the component that selects the correct component based on the current
 * tenant.  Components are lazily instantiated, and DisposableBeans destroy() methods are called when a tenant is
 * stopped.
 */
public class MultiTenantComponentFactoryBean implements FactoryBean, ApplicationContextAware, BeanNameAware
{
    private static final Logger log = LoggerFactory.getLogger(MultiTenantComponentFactoryBean.class);

    private Class[] interfaces;
    private Class implementation;
    private boolean lazyLoad = true;
    private Object proxy;
    private ApplicationContext applicationContext;
    private String name;

    public synchronized Object getObject() throws Exception
    {
        if (proxy == null)
        {
            MultiTenantComponentFactory factory = MultiTenantContext.getFactory();
            MultiTenantComponentMap.LazyLoadStrategy strategy;
            if (lazyLoad)
            {
                strategy = MultiTenantComponentMap.LazyLoadStrategy.LAZY_LOAD;
            }
            else
            {
                strategy = MultiTenantComponentMap.LazyLoadStrategy.EAGER_LOAD;
            }
            MultiTenantComponentMap map = factory.createComponentMapBuilder(new FactoryBeanCreator())
                    .setLazyLoad(strategy).construct();

            if (interfaces != null && interfaces.length > 0)
            {
                proxy = factory.createComponent(map, implementation.getClassLoader(), interfaces);
            }
            else
            {
                proxy = factory.createEnhancedComponent(map, implementation);
            }
        }
        return proxy;
    }

    public Class getObjectType()
    {
        if (interfaces == null)
        {
            return implementation;
        }
        else if (interfaces.length == 1)
        {
            return interfaces[0];
        }
        return null;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void setInterfaces(final Class[] interfaces)
    {
        this.interfaces = interfaces;
    }

    public void setImplementation(final Class implementation)
    {
        this.implementation = implementation;
    }

    public void setApplicationContext(final ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    public void setBeanName(final String name)
    {
        this.name = name;
    }

    public void setLazyLoad(final boolean lazyLoad)
    {
        this.lazyLoad = lazyLoad;
    }

    private class FactoryBeanCreator implements MultiTenantCreator<Object>, MultiTenantDestroyer<Object>
    {
        public Object create(final Tenant tenant)
        {
            return applicationContext.getAutowireCapableBeanFactory().createBean(implementation,
                    AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT, false);
        }

        public void destroy(final Tenant tenant, final Object instance)
        {
            if (instance instanceof DisposableBean)
            {
                try
                {
                    ((DisposableBean) instance).destroy();
                }
                catch (Exception e)
                {
                    log.error("Exception thrown while disposing bean " + name + " for tenant " + tenant.getName());
                }
            }
        }
    }


}
