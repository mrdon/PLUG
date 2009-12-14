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
        assertEquals("foo", list.get(0).getProperties().get("bean-name"));
        assertEquals(1, list.get(0).getMainInterfaces().length);
        assertEquals(BeanFactoryAware.class.getName(), list.get(0).getMainInterfaces()[0]);
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

    public void testProvideWithMultipleCustomInterfaces()
    {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("com/atlassian/plugin/spring/pluginns/plugins-spring-test-interface.xml"));

        HostComponentProvider provider = getHostProvider(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals("fooMultipleInterface", list.get(1).getProperties().get("bean-name"));
        assertEquals(2, list.get(1).getMainInterfaces().length);
        assertTrue(Arrays.asList(list.get(1).getMainInterfaces()).contains(BeanFactoryAware.class.getName()));
        assertTrue(Arrays.asList(list.get(1).getMainInterfaces()).contains(Barable.class.getName()));
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
