package com.atlassian.plugin.spring;

import com.atlassian.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.PropertyBuilder;
import com.atlassian.plugin.osgi.hostcomponents.impl.DefaultComponentRegistrar;
import junit.framework.TestCase;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.atlassian.plugin.spring.PluginBeanDefinitionRegistry.HOST_COMPONENT_PROVIDER;

public class TestSpringHostComponentProviderFactoryBeanWithXmlConfiguration extends TestCase
{
    private static final HashSet<Class> FOOABLE_BEAN_INTERFACES = new HashSet<Class>(Arrays.asList(Serializable.class, Map.class, Cloneable.class, Fooable.class, Barable.class));
    private static final HashSet<Class> FOO_BARABLE_INTERFACES = new HashSet<Class>(Arrays.asList(Fooable.class, Barable.class));

    public void testProvide()
    {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("com/atlassian/plugin/spring/pluginns/plugins-spring-test.xml"));

        HostComponentProvider provider = getHostProvider(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0).getProperties().get("bean-name"));
        assertEquals(5, list.get(0).getMainInterfaces().length);
        assertEquals(FOOABLE_BEAN_INTERFACES, new HashSet<Class>(Arrays.asList(list.get(0).getMainInterfaceClasses())));
        assertEquals(ContextClassLoaderStrategy.USE_PLUGIN.name(), list.get(0).getProperties().get(PropertyBuilder.CONTEXT_CLASS_LOADER_STRATEGY));
    }

    public void testProvideWithDeprecations()
    {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("com/atlassian/plugin/spring/pluginns/plugins-spring-deprecations.xml"));

        HostComponentProvider provider = getHostProvider(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0).getProperties().get("bean-name"));
        assertEquals(5, list.get(0).getMainInterfaces().length);
        assertEquals(FOOABLE_BEAN_INTERFACES, new HashSet<Class>(Arrays.asList(list.get(0).getMainInterfaceClasses())));
        assertEquals(ContextClassLoaderStrategy.USE_PLUGIN.name(), list.get(0).getProperties().get(PropertyBuilder.CONTEXT_CLASS_LOADER_STRATEGY));
    }

    private HostComponentProvider getHostProvider(BeanFactory factory)
    {
        final HostComponentProvider provider = (HostComponentProvider) factory.getBean(HOST_COMPONENT_PROVIDER);
        assertNotNull(provider);
        return provider;
    }

    public void testProvideWithPrototype()
    {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("com/atlassian/plugin/spring/pluginns/plugins-spring-test-prototype.xml"));

        HostComponentProvider provider = getHostProvider(factory);


        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0).getProperties().get("bean-name"));
        assertEquals(5, list.get(0).getMainInterfaces().length);
        assertEquals(FOOABLE_BEAN_INTERFACES, new HashSet<Class>(Arrays.asList(list.get(0).getMainInterfaceClasses())));
    }

    public void testProvideWithCustomInterface()
    {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("com/atlassian/plugin/spring/pluginns/plugins-spring-test-interface.xml"));

        HostComponentProvider provider = getHostProvider(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(2, list.size());

        if ("foo".equals(list.get(0).getProperties().get("bean-name")))
        {
            assertFoo(list.get(0));
            assertFooMultipleInterfaces(list.get(1));
        }
        else
        {
            assertFoo(list.get(1));
            assertFooMultipleInterfaces(list.get(0));
        }
    }

    private void assertFoo(HostComponentRegistration registration)
    {
        assertEquals("foo", registration.getProperties().get("bean-name"));
        assertEquals(1, registration.getMainInterfaces().length);
        assertEquals(BeanFactoryAware.class.getName(), registration.getMainInterfaces()[0]);
    }

    private void assertFooMultipleInterfaces(HostComponentRegistration registration)
    {
        assertEquals("fooMultipleInterface", registration.getProperties().get("bean-name"));
        assertEquals(2, registration.getMainInterfaces().length);
        assertEquals(BeanFactoryAware.class.getName(), registration.getMainInterfaces()[0]);
        assertEquals(Barable.class.getName(), registration.getMainInterfaces()[1]);
    }

    public void testProvideWithInterfaceOnSuperClass()
    {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("com/atlassian/plugin/spring/pluginns/plugins-spring-test-super-interface.xml"));

        HostComponentProvider provider = getHostProvider(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foobarable", list.get(0).getProperties().get("bean-name"));
        assertEquals(2, list.get(0).getMainInterfaces().length);
        assertEquals(FOO_BARABLE_INTERFACES, new HashSet<Class>(Arrays.asList(list.get(0).getMainInterfaceClasses())));
    }

    public void testProvideWithNestedContexts()
    {
        XmlBeanFactory parentFactory = new XmlBeanFactory(new ClassPathResource("com/atlassian/plugin/spring/pluginns/plugins-spring-test.xml"));
        XmlBeanFactory childFactory = new XmlBeanFactory(new ClassPathResource("com/atlassian/plugin/spring/pluginns/plugins-spring-test-child.xml"), parentFactory);

        HostComponentProvider provider = getHostProvider(childFactory);

        assertTrue(parentFactory.containsBeanDefinition(HOST_COMPONENT_PROVIDER));
        assertTrue(childFactory.containsBeanDefinition(HOST_COMPONENT_PROVIDER));


        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(2, list.size());
    }
}
