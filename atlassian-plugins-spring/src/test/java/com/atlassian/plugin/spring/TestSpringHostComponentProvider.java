package com.atlassian.plugin.spring;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.impl.DefaultComponentRegistrar;
import junit.framework.TestCase;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.aop.framework.ProxyFactory;
import org.aopalliance.aop.Advice;

import java.util.List;

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
        factory.addBean("bean", new Bean());
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

    public void testProvideWithProxy()
    {
        StaticListableBeanFactory factory = new StaticListableBeanFactory() {
            @Override
            public boolean isSingleton(String name) throws NoSuchBeanDefinitionException
            {
                return true;
            }
        };

        ProxyFactory pfactory = new ProxyFactory(new Bean());
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

    @AvailableToPlugins
    static class Bean implements Fooable {}

    static interface Fooable {}


}
