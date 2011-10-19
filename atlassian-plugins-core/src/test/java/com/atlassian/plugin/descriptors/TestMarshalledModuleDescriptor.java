package com.atlassian.plugin.descriptors;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.loaders.SinglePluginLoader;
import com.atlassian.plugin.manager.DefaultPluginManager;
import com.atlassian.plugin.manager.store.MemoryPluginPersistentStateStore;
import com.atlassian.plugin.module.ModuleFactory;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;

/**
 * @since version
 */
public class TestMarshalledModuleDescriptor
{
    @Test
    public void testMarshalledFields() throws Exception
    {
        DefaultPluginManager manager = createDefaultPluginManager("test-marshalled-descriptor-plugin.xml");

        // Test you can get the instances of a plugin point
        List<BookDescriptor> instances = manager.getEnabledModuleDescriptorsByClass(BookDescriptor.class);
        assertEquals(2, instances.size());

        BookDescriptor book1 = instances.get(0);
        BookDescriptor book2 = instances.get(1);

        assertEquals("Fighting With Goats", book1.getTitle());
        assertEquals("fwg", book1.getKey());
        assertEquals(5.0F,book1.getMinJavaVersion());
        assertEquals("test.atlassian.plugin:fwg", book1.getCompleteKey());

        assertEquals(null, book2.getTitle());
        assertEquals("no-goats", book2.getKey());
        assertEquals("test.atlassian.plugin:no-goats", book2.getCompleteKey());
    }

    private DefaultPluginManager createDefaultPluginManager(String atlassianPluginFileName)
    {
        // We need to mock a couple of dependencies
        HostContainer hostContainer = new DefaultHostContainer();

        MemoryPluginPersistentStateStore pluginStateStore = new MemoryPluginPersistentStateStore();
        List<PluginLoader> pluginLoaders = new ArrayList<PluginLoader>();
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(hostContainer);
        DefaultPluginEventManager pluginEventManager = new DefaultPluginEventManager();

        DefaultPluginManager manager = new DefaultPluginManager(pluginStateStore, pluginLoaders, moduleDescriptorFactory, pluginEventManager);
        moduleDescriptorFactory.addModuleDescriptor("book2", BookDescriptor.class);

        pluginLoaders.add(new SinglePluginLoader(atlassianPluginFileName));
        manager.init();

        return manager;
    }
}
