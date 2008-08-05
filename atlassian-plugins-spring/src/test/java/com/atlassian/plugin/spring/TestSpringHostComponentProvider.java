package com.atlassian.plugin.spring;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import com.atlassian.plugin.osgi.hostcomponents.PropertyBuilder;
import com.atlassian.plugin.osgi.hostcomponents.impl.DefaultComponentRegistrar;
import junit.framework.TestCase;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.aop.framework.ProxyFactory;
import org.aopalliance.aop.Advice;

import java.util.List;
import java.io.Serializable;

public class TestSpringHostComponentProvider extends TestCase
{
    public void testProvide()
    {
        StaticListableBeanFactory factory = new StaticListableBeanFactory() {
            @Override
            public boolean isSingleton(String name) throws NoSuchBeanDefinitionException
            {
                return true;
            }
        };
        factory.addBean("bean", new FooableBean());
        factory.addBean("string", "hello");

        SpringHostComponentProvider provider = new SpringHostComponentProvider();
        provider.setBeanFactory(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("bean", list.get(0).getProperties().get("bean-name"));
        assertEquals(5, list.get(0).getMainInterfaces().length);
    }

    public void testProvideWithCCLStrategy()
    {
        StaticListableBeanFactory factory = new StaticListableBeanFactory() {
            @Override
            public boolean isSingleton(String name) throws NoSuchBeanDefinitionException
            {
                return true;
            }
        };
        factory.addBean("bean", new FooablePluginService());
        factory.addBean("string", "hello");

        SpringHostComponentProvider provider = new SpringHostComponentProvider();
        provider.setBeanFactory(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("bean", list.get(0).getProperties().get(PropertyBuilder.BEAN_NAME));
        assertEquals(ContextClassLoaderStrategy.USE_PLUGIN.name(), list.get(0).getProperties().get(PropertyBuilder.CONTEXT_CLASS_LOADER_STRATEGY));


    }

    public void testProvideWithProxy()
    {
        StaticListableBeanFactory factory = new StaticListableBeanFactory() {
            @Override
            public boolean isSingleton(String name) throws NoSuchBeanDefinitionException
            {
                return true;
            }
        };

        ProxyFactory pfactory = new ProxyFactory(new FooableBean());
        pfactory.addInterface(Fooable.class);
        pfactory.addAdvice(new Advice() {});

        factory.addBean("bean", pfactory.getProxy());
        factory.addBean("string", "hello");

        SpringHostComponentProvider provider = new SpringHostComponentProvider();
        provider.setBeanFactory(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("bean", list.get(0).getProperties().get("bean-name"));
        assertEquals(Fooable.class.getName(), list.get(0).getMainInterfaces()[0]);


    }


}
