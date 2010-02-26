package com.atlassian.plugin.osgi;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.loaders.ClassPathPluginLoader;
import com.atlassian.plugin.module.ClassModuleCreator;
import com.atlassian.plugin.module.DefaultModuleClassFactory;
import com.atlassian.plugin.module.ModuleClassFactory;
import com.atlassian.plugin.module.ModuleCreator;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.module.SpringModuleCreator;
import com.atlassian.plugin.osgi.test.TestServlet;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.test.PluginJarBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests around the creation of the module class of {@link com.atlassian.plugin.ModuleDescriptor}
 */
public class TestPluginModuleClassCreation extends PluginInContainerTestBase
{
    public void testInstallPlugin2AndGetModuleClass() throws Exception
    {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        final File jar = firstBuilder
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <servlet key='foo' class='first.MyServlet'>",
                        "       <url-pattern>/foo</url-pattern>",
                        "    </servlet>",
                        "</atlassian-plugin>")
                .addFormattedJava("first.MyServlet",
                        "package first;",
                        "import javax.servlet.http.HttpServlet;",
                        "public class MyServlet extends javax.servlet.http.HttpServlet {",
                        "   public String getServletInfo() {",
                        "       return 'bob';",
                        "   }",
                        "}")
                .build();

        HostContainer hostContainer = mock(HostContainer.class);
        final ServletModuleManager servletModuleManager = mock(ServletModuleManager.class);
        final List<ModuleCreator> providers = new ArrayList<ModuleCreator>();
        ModuleCreator classModuleCreator = new ClassModuleCreator(hostContainer);
        providers.add(classModuleCreator);
        ModuleCreator springBeanModuleCreator = new SpringModuleCreator();
        providers.add(springBeanModuleCreator);

        final ModuleClassFactory moduleCreator = new DefaultModuleClassFactory(providers);
        when(hostContainer.create(StubServletModuleDescriptor.class)).thenReturn(new StubServletModuleDescriptor(moduleCreator, servletModuleManager));

        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(hostContainer);
        moduleDescriptorFactory.addModuleDescriptor("servlet", StubServletModuleDescriptor.class);

        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
            }
        }, moduleDescriptorFactory);

        pluginManager.installPlugin(new JarPluginArtifact(jar));

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("first.MyServlet", pluginManager.getPlugin("first").getModuleDescriptor("foo").getModule().getClass().getName());
    }

    public void testInstallPlugins1AndGetModuleClass() throws Exception
    {
        ClassPathPluginLoader classPathPluginLoader = new ClassPathPluginLoader("testInstallPlugins1AndGetModuleClass.xml");
        HostContainer hostContainer = mock(HostContainer.class);
        final ServletModuleManager servletModuleManager = mock(ServletModuleManager.class);
        final List<ModuleCreator> providers = new ArrayList<ModuleCreator>();
        ModuleCreator classModuleCreator = new ClassModuleCreator(hostContainer);
        providers.add(classModuleCreator);
        ModuleCreator springBeanModuleCreator = new SpringModuleCreator();
        providers.add(springBeanModuleCreator);

        final ModuleClassFactory moduleCreator = new DefaultModuleClassFactory(providers);
        when(hostContainer.create(StubServletModuleDescriptor.class)).thenReturn(new StubServletModuleDescriptor(moduleCreator, servletModuleManager));
        when(hostContainer.create(TestServlet.class)).thenReturn(new TestServlet());

        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(hostContainer);
        moduleDescriptorFactory.addModuleDescriptor("servlet", StubServletModuleDescriptor.class);

        initPluginManager(moduleDescriptorFactory, classPathPluginLoader);

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("com.atlassian.plugin.osgi.test.TestServlet", pluginManager.getPlugin("first").getModuleDescriptor("foo").getModule().getClass().getName());
    }

    public void testInstallPlugins1AndFailToGetModuleClassFromSpring() throws Exception
    {
        ClassPathPluginLoader classPathPluginLoader = new ClassPathPluginLoader("testInstallPlugins1AndFailToGetModuleClassFromSpring.xml");
        HostContainer hostContainer = mock(HostContainer.class);
        final ServletModuleManager servletModuleManager = mock(ServletModuleManager.class);
        final List<ModuleCreator> providers = new ArrayList<ModuleCreator>();
        ModuleCreator classModuleCreator = new ClassModuleCreator(hostContainer);
        providers.add(classModuleCreator);
        ModuleCreator springBeanModuleCreator = new SpringModuleCreator();
        providers.add(springBeanModuleCreator);

        final ModuleClassFactory moduleCreator = new DefaultModuleClassFactory(providers);
        when(hostContainer.create(StubServletModuleDescriptor.class)).thenReturn(new StubServletModuleDescriptor(moduleCreator, servletModuleManager));
        when(hostContainer.create(TestServlet.class)).thenReturn(new TestServlet());

        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(hostContainer);
        moduleDescriptorFactory.addModuleDescriptor("servlet", StubServletModuleDescriptor.class);

        initPluginManager(moduleDescriptorFactory, classPathPluginLoader);
        assertEquals(1, pluginManager.getPlugins().size());
        final Plugin plugin = pluginManager.getPlugins().iterator().next();
        assertTrue(plugin instanceof UnloadablePlugin);
        UnloadablePlugin unloadablePlugin = (UnloadablePlugin) plugin;
        assertEquals("There was a problem loading the module descriptor: A test servlet.<br/>Failed to resolve 'BeanServlet'. You cannot use 'bean' prefix with non-OSGi plugins", unloadablePlugin.getErrorText());
        assertEquals(0, pluginManager.getEnabledPlugins().size());
    }

    public void testInstallPlugins2AndGetModuleClassFromSpring() throws Exception
    {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        final File jar = firstBuilder
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <servlet key='foo' class='bean:obj'>",
                        "       <url-pattern>/foo</url-pattern>",
                        "    </servlet>",
                        "<component key='obj' class='first.MyServlet'/>",
                        "</atlassian-plugin>")
                .addFormattedJava("first.MyServlet",
                        "package first;",
                        "import javax.servlet.http.HttpServlet;",
                        "public class MyServlet extends javax.servlet.http.HttpServlet {",
                        "   public String getServletInfo() {",
                        "       return 'bob';",
                        "   }",
                        "}")
                .build();

        HostContainer hostContainer = mock(HostContainer.class);
        final ServletModuleManager servletModuleManager = mock(ServletModuleManager.class);
        final List<ModuleCreator> providers = new ArrayList<ModuleCreator>();
        ModuleCreator classModuleCreator = new ClassModuleCreator(hostContainer);
        providers.add(classModuleCreator);
        ModuleCreator springBeanModuleCreator = new SpringModuleCreator();
        providers.add(springBeanModuleCreator);

        final ModuleClassFactory moduleCreator = new DefaultModuleClassFactory(providers);
        when(hostContainer.create(StubServletModuleDescriptor.class)).thenReturn(new StubServletModuleDescriptor(moduleCreator, servletModuleManager));

        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(hostContainer);
        moduleDescriptorFactory.addModuleDescriptor("servlet", StubServletModuleDescriptor.class);

        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
            }
        }, moduleDescriptorFactory);

        pluginManager.installPlugin(new JarPluginArtifact(jar));

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("first.MyServlet", pluginManager.getPlugin("first").getModuleDescriptor("foo").getModule().getClass().getName());
    }

    public void testGetModuleClassFromComponentModuleDescriptor() throws Exception
    {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        final File jar = firstBuilder
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "<component key='obj' class='first.MyServlet'/>",
                        "</atlassian-plugin>")
                .addFormattedJava("first.MyServlet",
                        "package first;",
                        "import javax.servlet.http.HttpServlet;",
                        "public class MyServlet extends javax.servlet.http.HttpServlet {",
                        "   public String getServletInfo() {",
                        "       return 'bob';",
                        "   }",
                        "}")
                .build();


        initPluginManager();

        pluginManager.installPlugin(new JarPluginArtifact(jar));

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("first.MyServlet", pluginManager.getPlugin("first").getModuleDescriptor("obj").getModule().getClass().getName());
    }

    public void testGetModuleClassFromComponentImportModuleDescriptor() throws Exception
    {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        final File jar1 = firstBuilder
                        .addFormattedResource("atlassian-plugin.xml",
                                "<atlassian-plugin name='Test1' key='first' pluginsVersion='2'>",
                                "    <plugin-info>",
                                "        <version>1.0</version>",
                                "    </plugin-info>",
                                "<component key='obj' class='first.MyServlet' public='true'>",
                                "<interface>com.atlassian.plugin.osgi.SomeInterface</interface>",
                                "</component>",
                                "</atlassian-plugin>")
                        .addFormattedJava("com.atlassian.plugin.osgi.SomeInterface",
                            "package com.atlassian.plugin.osgi;",
                            "public interface SomeInterface {}")
                        .addFormattedJava("first.MyServlet",
                                "package first;",
                                "import javax.servlet.http.HttpServlet;",
                                "public class MyServlet extends javax.servlet.http.HttpServlet implements com.atlassian.plugin.osgi.SomeInterface {",
                                "   public String getServletInfo() {",
                                "       return 'bob';",
                                "   }",
                                "}")
                        .build();

        final File jar2 = new PluginJarBuilder("second")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test2' key='second' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='obj' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "</atlassian-plugin>"
                )
                .addFormattedJava("com.atlassian.plugin.osgi.SomeInterface",
                            "package com.atlassian.plugin.osgi;",
                            "public interface SomeInterface {}")
                .build();

        initPluginManager();
        pluginManager.installPlugin(new JarPluginArtifact(jar1));
        pluginManager.installPlugin(new JarPluginArtifact(jar2));


        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertEquals("first.MyServlet", pluginManager.getPlugin("first").getModuleDescriptor("obj").getModule().getClass().getName());
        assertTrue(pluginManager.getPlugin("second").getModuleDescriptor("obj").getModule() instanceof SomeInterface);
    }

    public void testFailToGetModuleClassFromSpring() throws Exception
    {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        final File jar = firstBuilder
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <servlet key='foo' class='bean:beanId' name='spring bean for servlet'>",
                        "       <url-pattern>/foo</url-pattern>",
                        "    </servlet>",
                        "<component key='obj' class='first.MyServlet' />",
                        "</atlassian-plugin>")
                .addFormattedJava("first.MyServlet",
                        "package first;",
                        "import javax.servlet.http.HttpServlet;",
                        "public class MyServlet extends javax.servlet.http.HttpServlet {",
                        "   public String getServletInfo() {",
                        "       return 'bob';",
                        "   }",
                        "}")
                .build();

        HostContainer hostContainer = mock(HostContainer.class);
        final ServletModuleManager servletModuleManager = mock(ServletModuleManager.class);
        final List<ModuleCreator> providers = new ArrayList<ModuleCreator>();
        ModuleCreator classModuleCreator = new ClassModuleCreator(hostContainer);
        providers.add(classModuleCreator);
        ModuleCreator springBeanModuleCreator = new SpringModuleCreator();
        providers.add(springBeanModuleCreator);

        final ModuleClassFactory moduleCreator = new DefaultModuleClassFactory(providers);
        when(hostContainer.create(StubServletModuleDescriptor.class)).thenReturn(new StubServletModuleDescriptor(moduleCreator, servletModuleManager));

        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(hostContainer);
        moduleDescriptorFactory.addModuleDescriptor("servlet", StubServletModuleDescriptor.class);

        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
            }
        }, moduleDescriptorFactory);

        pluginManager.installPlugin(new JarPluginArtifact(jar));
        assertEquals(0, pluginManager.getEnabledPlugins().size());
        final Plugin plugin = pluginManager.getPlugins().iterator().next();
        assertTrue(plugin instanceof UnloadablePlugin);
        UnloadablePlugin unloadablePlugin = (UnloadablePlugin) plugin;
        assertEquals("There was a problem loading the descriptor for module 'spring bean for servlet' in plugin 'Test'.\n"
                + " Couldn't find the spring bean reference with the id 'beanId'. Please make sure you have defined a spring bean with this id within this plugin. Either using a native spring configuration or the component module descriptor, the spring bean id is the key of the module descriptor.If the spring bean you refer to is not part of this plugin, please make sure it is declared as public so it is visible to other plugins.", unloadablePlugin.getErrorText());

    }
}
