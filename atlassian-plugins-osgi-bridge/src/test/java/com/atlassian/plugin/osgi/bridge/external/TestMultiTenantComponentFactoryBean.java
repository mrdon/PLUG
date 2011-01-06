package com.atlassian.plugin.osgi.bridge.external;

import com.atlassian.multitenant.MultiTenantComponentFactory;
import com.atlassian.multitenant.MultiTenantComponentMap;
import com.atlassian.multitenant.MultiTenantComponentMapBuilder;
import com.atlassian.multitenant.MultiTenantContext;
import com.atlassian.multitenant.MultiTenantCreator;
import com.atlassian.multitenant.MultiTenantDestroyer;
import junit.framework.TestCase;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 */
public class TestMultiTenantComponentFactoryBean extends TestCase
{
    @Mock
    private AutowireCapableBeanFactory beanFactory;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private MultiTenantComponentFactory factory;
    @Mock
    private MultiTenantComponentMap map;
    @Mock
    private MultiTenantComponentMapBuilder builder;

    private MultiTenantComponentFactoryBean factoryBean;

    @Override
    protected void setUp() throws Exception
    {
        initMocks(this);

        MultiTenantContext.setFactory(factory);
        factoryBean = new MultiTenantComponentFactoryBean();
        factoryBean.setApplicationContext(applicationContext);
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(beanFactory);
        factoryBean.setBeanName("beanName");
    }

    public void testComponentWithInterfaces() throws Exception
    {
        ArrayList theProxy = new ArrayList();

        factoryBean.setInterfaces(new Class[] { List.class});
        factoryBean.setImplementation(ArrayList.class);
        factoryBean.setLazyLoad(true);
        ArgumentCaptor<MultiTenantCreator> captor = ArgumentCaptor.forClass(MultiTenantCreator.class);
        when(factory.createComponentMapBuilder(captor.capture())).thenReturn(builder);
        when(builder.setLazyLoad(MultiTenantComponentMap.LazyLoadStrategy.LAZY_LOAD)).thenReturn(builder);
        when(builder.construct()).thenReturn(map);
        when(factory.createComponent(map, ArrayList.class.getClassLoader(), List.class)).thenReturn(theProxy);
        assertSame(theProxy, factoryBean.getObject());

        // Ensure that the creator is correct
        MultiTenantCreator<List> creator = captor.getValue();
        ArrayList theObject = new ArrayList();
        when(beanFactory.createBean(ArrayList.class, AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT, false)).thenReturn(theObject);
        assertSame(theObject, creator.create(null));
        assertTrue(creator instanceof MultiTenantDestroyer);
    }

    public void testComponentNoInterfaces() throws Exception
    {
        Object theProxy = new Object();

        factoryBean.setImplementation(Object.class);
        factoryBean.setLazyLoad(true);
        ArgumentCaptor<MultiTenantCreator> captor = ArgumentCaptor.forClass(MultiTenantCreator.class);
        when(factory.createComponentMapBuilder(captor.capture())).thenReturn(builder);
        when(builder.setLazyLoad(MultiTenantComponentMap.LazyLoadStrategy.LAZY_LOAD)).thenReturn(builder);
        when(builder.construct()).thenReturn(map);
        when(factory.createEnhancedComponent(map, Object.class)).thenReturn(theProxy);
        assertSame(theProxy, factoryBean.getObject());

        // Ensure that the creator is correct
        MultiTenantCreator<Object> creator = captor.getValue();
        Object theObject = new Object();
        when(beanFactory.createBean(Object.class, AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT, false)).thenReturn(theObject);
        assertSame(theObject, creator.create(null));
        assertTrue(creator instanceof MultiTenantDestroyer);
    }

    public void testEagerLoad() throws Exception
    {
        Object theProxy = new Object();

        factoryBean.setImplementation(Object.class);
        factoryBean.setLazyLoad(false);
        ArgumentCaptor<MultiTenantCreator> captor = ArgumentCaptor.forClass(MultiTenantCreator.class);
        when(factory.createComponentMapBuilder(captor.capture())).thenReturn(builder);
        when(builder.setLazyLoad(MultiTenantComponentMap.LazyLoadStrategy.EAGER_LOAD)).thenReturn(builder);
        when(builder.construct()).thenReturn(map);
        when(factory.createEnhancedComponent(map, Object.class)).thenReturn(theProxy);
        assertSame(theProxy, factoryBean.getObject());
    }

    public void testDisposable() throws Exception
    {
        Object theProxy = new Object();

        factoryBean.setImplementation(Object.class);
        factoryBean.setLazyLoad(true);
        ArgumentCaptor<MultiTenantCreator> captor = ArgumentCaptor.forClass(MultiTenantCreator.class);
        when(factory.createComponentMapBuilder(captor.capture())).thenReturn(builder);
        when(builder.setLazyLoad(MultiTenantComponentMap.LazyLoadStrategy.LAZY_LOAD)).thenReturn(builder);
        when(builder.construct()).thenReturn(map);
        when(factory.createEnhancedComponent(map, Object.class)).thenReturn(theProxy);
        assertSame(theProxy, factoryBean.getObject());

        // Ensure that the creator is correct
        MultiTenantCreator<Object> creator = captor.getValue();
        DisposableBean theObject = mock(DisposableBean.class);
        when(beanFactory.createBean(Object.class, AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT, false)).thenReturn(theObject);
        assertSame(theObject, creator.create(null));
        assertTrue(creator instanceof MultiTenantDestroyer);
        MultiTenantDestroyer destroyer = (MultiTenantDestroyer<Object>) creator;
        destroyer.destroy(null, theObject);
        verify(theObject).destroy();
    }

    public void testGetObjectTypeNoInterfaces()
    {
        factoryBean.setImplementation(Object.class);
        assertEquals(Object.class, factoryBean.getObjectType());
    }

    public void testGetObjectTypeOneInterface()
    {
        factoryBean.setInterfaces(new Class[] {List.class});
        factoryBean.setImplementation(ArrayList.class);
        assertEquals(List.class, factoryBean.getObjectType());
    }

    public void testGetObjectTypeManyInterfaces()
    {
        factoryBean.setInterfaces(new Class[] {List.class, Set.class});
        factoryBean.setImplementation(ArrayList.class);
        assertNull(factoryBean.getObjectType());
    }

}
