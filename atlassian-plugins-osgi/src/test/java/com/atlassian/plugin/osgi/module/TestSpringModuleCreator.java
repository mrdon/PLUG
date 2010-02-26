package com.atlassian.plugin.osgi.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.impl.AbstractPlugin;
import com.atlassian.plugin.module.ContainerAccessor;
import com.atlassian.plugin.module.ContainerManagedPlugin;
import com.atlassian.plugin.module.ModuleCreator;
import com.atlassian.plugin.osgi.spring.SpringContainerAccessor;
import junit.framework.TestCase;

import java.io.InputStream;
import java.net.URL;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestSpringModuleCreator extends TestCase
{
    ModuleCreator moduleCreator;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        moduleCreator = new SpringModuleCreator();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testSupportsPrefix() throws Exception
    {
        assertEquals("bean", moduleCreator.getPrefix());
    }

    public void testCreateBeanFailedUsingHostContainer() throws Exception
    {
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        final Plugin plugin = mock(Plugin.class);
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);

        try
        {
            moduleCreator.createModule("springBean", moduleDescriptor);
            fail("Spring not available for non osgi plugins. Bean creation should have failed");
        }
        catch(IllegalArgumentException e)
        {
            assertEquals("Failed to resolve 'springBean'. You cannot use 'bean' prefix with non-OSGi plugins", e.getMessage());
        }
    }

    public void testCreateBeanUsingSpring() throws Exception
    {
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        final SpringContainerAccessor springContextAccessor = mock(SpringContainerAccessor.class);
        final Plugin plugin = new MockContainerManagedPlugin(springContextAccessor);
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        final Object springBean = new Object();
        when(springContextAccessor.getBean("springBean")).thenReturn(springBean);
        final Object obj = moduleCreator.createModule("springBean", moduleDescriptor);
        verify(springContextAccessor).getBean("springBean");
        assertEquals(obj, springBean);
    }

    private class MockContainerManagedPlugin extends AbstractPlugin implements ContainerManagedPlugin
    {
        private ContainerAccessor containerAccessor;

        public MockContainerManagedPlugin(ContainerAccessor containerAccessor)
        {
            this.containerAccessor = containerAccessor;
        }

        public ContainerAccessor getContainerAccessor()
        {
            return containerAccessor;
        }

        public boolean isUninstallable()
        {
            return false;
        }

        public boolean isDeleteable()
        {
            return false;
        }

        public boolean isDynamicallyLoaded()
        {
            return false;
        }

        public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException
        {
            return (Class<T>) Class.forName(clazz);
        }

        public ClassLoader getClassLoader()
        {
            return null;
        }

        public URL getResource(final String path)
        {
            return null;
        }

        public InputStream getResourceAsStream(final String name)
        {
            return null;
        }
    }

}
