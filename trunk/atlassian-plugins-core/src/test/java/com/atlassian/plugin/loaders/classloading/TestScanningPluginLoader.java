package com.atlassian.plugin.loaders.classloading;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginArtifactFactory;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.loaders.ScanningPluginLoader;
import junit.framework.TestCase;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.URI;

public class TestScanningPluginLoader extends TestCase
{

    private PluginEventManager pluginEventManager;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        pluginEventManager = new DefaultPluginEventManager();
    }

    public void testOnShutdown()
    {
        PluginArtifactFactory artFactory = mock(PluginArtifactFactory.class);
        PluginArtifact art = mock(PluginArtifact.class);
        when(artFactory.create((URI)anyObject())).thenReturn(art);

        DeploymentUnit unit = new DeploymentUnit(new File("foo.jar"));
        Scanner scanner = mock(Scanner.class);
        when(scanner.getDeploymentUnits()).thenReturn(Collections.singletonList(unit));
        PluginFactory factory = mock(PluginFactory.class);
        Plugin plugin = mock(Plugin.class);
        when(plugin.isUninstallable()).thenReturn(true);

        when(factory.canCreate(art)).thenReturn("foo");
        when(factory.create(art, null)).thenReturn(plugin);

        ScanningPluginLoader loader = new ScanningPluginLoader(scanner, Arrays.asList(factory), artFactory, pluginEventManager);
        loader.loadAllPlugins(null);
        loader.onShutdown(null);
        verify(plugin).uninstall();
    }

    public void testOnShutdownButUninstallable()
    {
        PluginArtifactFactory artFactory = mock(PluginArtifactFactory.class);
        PluginArtifact art = mock(PluginArtifact.class);
        when(artFactory.create((URI)anyObject())).thenReturn(art);

        DeploymentUnit unit = new DeploymentUnit(new File("foo.jar"));
        Scanner scanner = mock(Scanner.class);
        when(scanner.getDeploymentUnits()).thenReturn(Collections.singletonList(unit));
        PluginFactory factory = mock(PluginFactory.class);
        Plugin plugin = mock(Plugin.class);
        when(plugin.isUninstallable()).thenReturn(false);

        when(factory.canCreate(art)).thenReturn("foo");
        when(factory.create(art, null)).thenReturn(plugin);

        ScanningPluginLoader loader = new ScanningPluginLoader(scanner, Arrays.asList(factory), artFactory, pluginEventManager);
        loader.loadAllPlugins(null);
        loader.onShutdown(null);
        verify(plugin, never()).uninstall();
    }

    public void testPostProcessCalledAlways()
    {
        final AtomicBoolean called = new AtomicBoolean(false);
        PluginArtifactFactory artFactory = mock(PluginArtifactFactory.class);
        PluginArtifact art = mock(PluginArtifact.class);
        when(artFactory.create((URI)anyObject())).thenReturn(art);

        DeploymentUnit unit = new DeploymentUnit(new File("foo.jar"));
        Scanner scanner = mock(Scanner.class);
        when(scanner.getDeploymentUnits()).thenReturn(Collections.singletonList(unit));
        PluginFactory factory = mock(PluginFactory.class);
        Plugin plugin = mock(Plugin.class);
        when(plugin.isUninstallable()).thenReturn(true);

        when(factory.canCreate(art)).thenReturn("foo");
        when(factory.create(art, null)).thenReturn(plugin);
        ScanningPluginLoader loader = new ScanningPluginLoader(scanner, Arrays.asList(factory), artFactory, pluginEventManager)
        {
            @Override
            protected Plugin postProcess(Plugin plugin)
            {
                called.set(true);
                return plugin;
            }
        };
        loader.loadAllPlugins(null);
        assertTrue(called.get());

        called.set(false);
        DeploymentUnit unit2 = new DeploymentUnit(new File("bar.jar"));
        when(scanner.scan()).thenReturn(Collections.singletonList(unit2));
        PluginArtifact art2 = mock(PluginArtifact.class);
        when(artFactory.create((URI)anyObject())).thenReturn(art2);
        when(factory.canCreate(art2)).thenReturn("bar");
        when(factory.create(art2, null)).thenReturn(plugin);
        loader.addFoundPlugins(null);
        assertTrue(called.get());
    }
}
