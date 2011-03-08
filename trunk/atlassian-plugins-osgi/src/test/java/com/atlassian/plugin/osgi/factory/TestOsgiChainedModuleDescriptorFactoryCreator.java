package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor;
import com.atlassian.plugin.osgi.external.ListableModuleDescriptorFactory;
import com.atlassian.plugin.osgi.factory.descriptor.ComponentModuleDescriptor;
import junit.framework.TestCase;
import org.osgi.util.tracker.ServiceTracker;

import java.util.HashSet;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TestOsgiChainedModuleDescriptorFactoryCreator extends TestCase
{
    private OsgiChainedModuleDescriptorFactoryCreator.ServiceTrackerFactory serviceTrackerFactory;
    private ServiceTracker tracker;
    private OsgiChainedModuleDescriptorFactoryCreator.ResourceLocator resourceLocator;
    private ModuleDescriptorFactory moduleDescriptorFactory;
    private OsgiChainedModuleDescriptorFactoryCreator creator;
    private ModuleDescriptor fooModuleDescriptor;

    public void testCreate() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        ModuleDescriptor dynModuleDescriptor = mock(ModuleDescriptor.class);
        ModuleDescriptorFactory dynModuleDescriptorFactory = createModuleDescriptorFactory("dyn", dynModuleDescriptor);
        when(tracker.getServices()).thenReturn(new Object[] {dynModuleDescriptorFactory});
        ModuleDescriptorFactory createdFactory = creator.create(resourceLocator, moduleDescriptorFactory);
        assertNotNull(createdFactory);
        assertEquals(ComponentModuleDescriptor.class, createdFactory.getModuleDescriptor("component").getClass());
        assertEquals(dynModuleDescriptor, createdFactory.getModuleDescriptor("dyn"));
        assertEquals(fooModuleDescriptor, createdFactory.getModuleDescriptor("foo"));
        assertEquals(UnrecognisedModuleDescriptor.class, createdFactory.getModuleDescriptor("unknown").getClass());
    }

    public void testCreateWithOsgiDescriptorOverridingHost() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        fooModuleDescriptor = mock(ModuleDescriptor.class);
        moduleDescriptorFactory = createModuleDescriptorFactory("component", fooModuleDescriptor);
        when(tracker.getServices()).thenReturn(new Object[0]);
        ModuleDescriptorFactory createdFactory = creator.create(resourceLocator, moduleDescriptorFactory);
        assertNotNull(createdFactory);
        assertEquals(ComponentModuleDescriptor.class, createdFactory.getModuleDescriptor("component").getClass());
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        serviceTrackerFactory = mock(OsgiChainedModuleDescriptorFactoryCreator.ServiceTrackerFactory.class);
        tracker = mock(ServiceTracker.class);
        when(serviceTrackerFactory.create(ModuleDescriptorFactory.class.getName())).thenReturn(tracker);
        resourceLocator = mock(OsgiChainedModuleDescriptorFactoryCreator.ResourceLocator.class);
        fooModuleDescriptor = mock(ModuleDescriptor.class);
        moduleDescriptorFactory = createModuleDescriptorFactory("foo", fooModuleDescriptor);
        creator = new OsgiChainedModuleDescriptorFactoryCreator(serviceTrackerFactory);
    }

    private ModuleDescriptorFactory createModuleDescriptorFactory(String prefix, ModuleDescriptor moduleDescriptor)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        ListableModuleDescriptorFactory moduleDescriptorFactory = mock(ListableModuleDescriptorFactory.class);
        when(moduleDescriptorFactory.hasModuleDescriptor(prefix)).thenReturn(true);
        when(moduleDescriptorFactory.getModuleDescriptor(prefix)).thenReturn(moduleDescriptor);
        when(moduleDescriptorFactory.getModuleDescriptorClasses()).thenReturn(new HashSet(asList(moduleDescriptor.getClass())));
        return moduleDescriptorFactory;
    }
}
