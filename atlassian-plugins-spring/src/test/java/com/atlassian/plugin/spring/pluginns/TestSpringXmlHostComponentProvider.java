package com.atlassian.plugin.spring.pluginns;

import junit.framework.TestCase;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import com.atlassian.plugin.spring.SpringHostComponentProvider;
import com.atlassian.plugin.spring.AvailableToPlugins;
import com.atlassian.plugin.spring.Fooable;
import com.atlassian.plugin.osgi.hostcomponents.impl.DefaultComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

import java.util.List;

public class TestSpringXmlHostComponentProvider extends TestCase
{
    public void testProvide()
    {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("com/atlassian/plugin/spring/pluginns/plugins-spring-test.xml"));

        SpringXmlHostComponentProvider provider = (SpringXmlHostComponentProvider) factory.getBean("hostComponentProvider");
        assertNotNull(provider);


        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0).getProperties().get("bean-name"));
        assertEquals(Fooable.class.getName(), list.get(0).getMainInterfaces()[0]);
    }

}
