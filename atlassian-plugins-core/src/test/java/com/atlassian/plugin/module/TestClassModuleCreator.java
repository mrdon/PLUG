package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.hostcontainer.HostContainer;
import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestClassModuleCreator extends TestCase
{
    ModuleCreator moduleCreator;
    private HostContainer hostContainer;

    @Override
    protected void setUp() throws Exception
    {
        hostContainer = mock(HostContainer.class);
        moduleCreator = new ClassModuleCreator(hostContainer);
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testSupportsPrefix() throws Exception
    {
        assertEquals("class",moduleCreator.getPrefix());
    }

    public void testCreateBeanUsingHostContainer() throws Exception
    {
        final ModuleDescriptor<Object> moduleDescriptor = mock(ModuleDescriptor.class);
        final Plugin plugin = mock(Plugin.class);

        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        when(moduleDescriptor.getModuleClass()).thenReturn(Object.class);
        final Object object = new Object();
        when(hostContainer.create(Object.class)).thenReturn(object);

        final Object bean = moduleCreator.createBean("myBean", moduleDescriptor);
        assertEquals(object, bean);
    }

    public void testCreateBeanUsingPluginContainer() throws Exception
    {
        final ModuleDescriptor<Object> moduleDescriptor = mock(ModuleDescriptor.class);
        final ContainerAccessor containerAccessor = mock(ContainerAccessor.class);
        final Plugin plugin = new MockContainerManagedPlugin(containerAccessor);

        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        final Object beanObject = new Object();
        when(containerAccessor.createBean(Object.class)).thenReturn(beanObject);
        final Object bean = moduleCreator.createBean("java.lang.Object", moduleDescriptor);
        assertEquals(beanObject, bean);
    }

}
