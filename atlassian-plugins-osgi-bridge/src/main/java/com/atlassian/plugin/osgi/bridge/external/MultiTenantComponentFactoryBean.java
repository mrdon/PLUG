package com.atlassian.plugin.osgi.bridge.external;

import com.atlassian.multitenant.MultiTenantAwareComponentCreator;
import com.atlassian.multitenant.MultiTenantAwareComponentDestroyer;
import com.atlassian.multitenant.MultiTenantContext;
import com.atlassian.multitenant.MultiTenantDescriptor;
import org.apache.log4j.Logger;
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
    private static final Logger log = Logger.getLogger(MultiTenantComponentFactoryBean.class);

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
            if (interfaces != null && interfaces.length > 0)
            {
                proxy = MultiTenantContext.getFactory().createComponent(new FactoryBeanCreator(),
                        implementation.getClassLoader(), lazyLoad, interfaces);
            }
            else
            {
                proxy = MultiTenantContext.getFactory().createEnhancedComponent(new FactoryBeanCreator(), implementation);
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

    private class FactoryBeanCreator
            implements MultiTenantAwareComponentCreator<Object>, MultiTenantAwareComponentDestroyer<Object>
    {
        public Object create(final MultiTenantDescriptor descriptor)
        {
            return applicationContext.getAutowireCapableBeanFactory().createBean(implementation,
                    AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT, false);
        }

        public void destroy(final MultiTenantDescriptor descriptor, final Object instance)
        {
            if (instance instanceof DisposableBean)
            {
                try
                {
                    ((DisposableBean) instance).destroy();
                }
                catch (Exception e)
                {
                    log.error("Exception thrown while disposing bean " + name + " for tenant " + descriptor.getName());
                }
            }
        }
    }


}
