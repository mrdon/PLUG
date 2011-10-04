/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 4:28:18 PM
 */
package com.atlassian.plugin.descriptors;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.loaders.SinglePluginLoader;
import com.atlassian.plugin.manager.DefaultPluginManager;
import com.atlassian.plugin.manager.store.MemoryPluginPersistentStateStore;
import com.atlassian.plugin.module.ModuleFactory;

public class TestPluginPointModuleDescriptor extends TestCase
{

    public void testPluginPointParsesJaxb() throws PluginParseException, DocumentException
    {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        MyPluginPoint descriptor = new MyPluginPoint(moduleFactory);

        descriptor.init(new StaticPlugin(),
                DocumentHelper.parseText("<book key=\"key\">" +
                        "<title>Raising Children</title>" +
                        "</book>").getRootElement());

        // Check it was properly parsed
        Book bean = descriptor.getModule();
        assertNotNull(bean);
        assertEquals("Raising Children", bean.getTitle());
    }

    public void testPluginPointParsesJaxbWithNoChild() throws PluginParseException, DocumentException
    {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        MyPluginPoint descriptor = new MyPluginPoint(moduleFactory);

        descriptor.init(new StaticPlugin(),
                DocumentHelper.parseText("<book key=\"key\" />").getRootElement());

        // Check it was properly parsed, with no title given
        Book bean = descriptor.getModule();
        assertNotNull(bean);
        assertEquals(null, bean.getTitle());
    }

    public void testGetInstances() throws PluginParseException, DocumentException
    {
        // Test preparation
        // We need to give a valid PluginAccessor to PluginPoint.getInstances(). DefaultPluginManager is a valid one,
        // but it requires mocking a couple of dependencies.
        DefaultPluginManager manager = createDefaultPluginManager("test-atlassian-plugin-point.xml");

        // Test you can get the instances of a plugin point
        List<? extends Book> instances = PluginPoint.getInstances(MyPluginPoint.class, manager);
        assertEquals(2, instances.size());
        assertNotSame(instances.get(0), instances.get(1));

        Book book = (instances.get(0) instanceof Book2 ? instances.get(1) : instances.get(0));
        assertEquals("Fighting With Bears", book.getTitle());
    }

    /**
     * Test providing a JavaBean (Book2) instead of describing the instance in the atlassian-plugin.xml
     */
    public void testGetInstancesUsingJavaBean() throws PluginParseException, DocumentException
    {
        // Test preparation
        // We need to give a valid PluginAccessor to PluginPoint.getInstances(). DefaultPluginManager is a valid one,
        // but it requires mocking a couple of dependencies.
        DefaultPluginManager manager = createDefaultPluginManager("test-atlassian-plugin-point.xml");

        // Test you can get the instances of a plugin point
        List<? extends Book> instances = PluginPoint.getInstances(MyPluginPoint.class, manager);
        assertEquals(2, instances.size());
        assertNotSame(instances.get(0), instances.get(1));

        Book2 book2 = (Book2) (instances.get(0) instanceof Book2 ? instances.get(0) : instances.get(1));
        assertEquals("Over The Counter", book2.getTitle());
    }

    public void testGetInstancesWithEmptyPlugin() throws PluginParseException, DocumentException
    {
        // Test preparation
        // We need to give a valid PluginAccessor to PluginPoint.getInstances(). DefaultPluginManager is a valid one,
        // but it requires mocking a couple of dependencies.
        DefaultPluginManager manager = createDefaultPluginManager("test-bad-plugin.xml");

        // Test getInstances returns an empty list when there's no such instance in the descriptor
        List<? extends Book> instances = PluginPoint.getInstances(MyPluginPoint.class, manager);
        assertEquals(0, instances.size());
    }

    private DefaultPluginManager createDefaultPluginManager(String atlassianPluginFileName)
    {
        // We need to mock a couple of dependencies
        DefaultPluginEventManager pluginEventManager = new DefaultPluginEventManager();
        MemoryPluginPersistentStateStore pluginStateStore = new MemoryPluginPersistentStateStore();
        List<PluginLoader> pluginLoaders = new ArrayList<PluginLoader>();
        HostContainer hostContainer = Mockito.mock(HostContainer.class);
        
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(hostContainer);
        DefaultPluginManager manager = new DefaultPluginManager(pluginStateStore, pluginLoaders, moduleDescriptorFactory, pluginEventManager);
        moduleDescriptorFactory.addModuleDescriptor("book", MyPluginPoint.class);

        ModuleFactory moduleFactory = Mockito.mock(ModuleFactory.class);
        Mockito.when(moduleFactory.createModule(Matchers.eq("com.atlassian.plugin.descriptors.Book2"), (ModuleDescriptor<Book>) Matchers.any()))
                .thenReturn(new com.atlassian.plugin.descriptors.Book2());
        // There are 2 calls to create() and they should return 2 separate instances
        Mockito.when(hostContainer.create((Class<MyPluginPoint>) Matchers.any())).thenReturn(new MyPluginPoint(moduleFactory)).thenReturn(new MyPluginPoint(moduleFactory));
        
        pluginLoaders.add(new SinglePluginLoader(atlassianPluginFileName));        
        manager.init();
        return manager;
    }
}
