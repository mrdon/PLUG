package com.atlassian.plugin.osgi;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

import java.io.File;
import java.util.List;
import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class PluginInstallTest extends PluginInContainerTestBase
{

    public void testUpgradeWithNewComponentImports() throws Exception
    {
        DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory();
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider(){
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface(){});
                registrar.register(AnotherInterface.class).forInstance(new AnotherInterface(){});
            }
        }, factory);

        File pluginJar = new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "    <dummy key='dum1'/>",
                        "</atlassian-plugin>")
                .build();
        File pluginJar2 = new PluginJarBuilder("second")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "    <component-import key='comp2' interface='com.atlassian.plugin.osgi.AnotherInterface' />",
                        "    <dummy key='dum1'/>",
                        "    <dummy key='dum2'/>",
                        "</atlassian-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test", pluginManager.getPlugin("test.plugin").getName());
        assertEquals(1, pluginManager.getPlugin("test.plugin").getModuleDescriptors().size());
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals(2, pluginManager.getPlugin("test.plugin").getModuleDescriptors().size());
        assertEquals("Test 2", pluginManager.getPlugin("test.plugin").getName());
    }

    public void testDynamicPluginModule() throws Exception
    {
        initPluginManager(new HostComponentProvider(){public void provide(ComponentRegistrar registrar){}});

        File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='factory' class='foo.MyModuleDescriptorFactory' public='true'>",
                        "       <interface>com.atlassian.plugin.ModuleDescriptorFactory</interface>",
                        "    </component>",
                        "</atlassian-plugin>")
                .addJava("foo.MyModuleDescriptor",
                        "package foo;" +
                        "public class MyModuleDescriptor extends com.atlassian.plugin.descriptors.AbstractModuleDescriptor {" +
                        "  public Object getModule(){return null;}" +
                        "}")
                .addJava("foo.MyModuleDescriptorFactory",
                        "package foo;" +
                        "public class MyModuleDescriptorFactory extends com.atlassian.plugin.DefaultModuleDescriptorFactory {" +
                        "  public MyModuleDescriptorFactory() {" +
                        "    super();" +
                        "    addModuleDescriptor(\"foo\", MyModuleDescriptor.class);" +
                        "  }" +
                        "}")
                .build();
        File pluginJar2 = new PluginJarBuilder("fooUser")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</atlassian-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
        ModuleDescriptor descriptor = descriptors.iterator().next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptor() throws Exception
    {
        initPluginManager(new HostComponentProvider(){public void provide(ComponentRegistrar registrar){}});

        File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "</atlassian-plugin>")
                .addJava("foo.MyModuleDescriptor",
                        "package foo;" +
                        "public class MyModuleDescriptor extends com.atlassian.plugin.descriptors.AbstractModuleDescriptor {" +
                        "  public Object getModule(){return null;}" +
                        "}")
                .build();
        File pluginJar2 = new PluginJarBuilder("fooUser")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</atlassian-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        Thread.sleep(5000);
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
        ModuleDescriptor descriptor = descriptors.iterator().next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicModuleDescriptor() throws Exception
    {
        initPluginManager(null);

        File pluginJar = new PluginJarBuilder("pluginType")
                .addPluginInformation("test.plugin", "foo", "1.0")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        BundleContext ctx = ((OsgiPlugin) pluginManager.getPlugin("test.plugin")).getBundle().getBundleContext();
        ServiceRegistration reg = ctx.registerService(ModuleDescriptor.class.getName(), new DummyWebItemModuleDescriptor(), null);

        Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
        ModuleDescriptor descriptor = descriptors.iterator().next();
        assertEquals("DummyWebItemModuleDescriptor", descriptor.getClass().getSimpleName());
        List<WebItemModuleDescriptor> list = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
        assertEquals(1, list.size());
        reg.unregister();
        list = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
        assertEquals(0, list.size());
    }

    public void testDynamicModuleDescriptorIsolatedToPlugin() throws Exception
    {
        initPluginManager(null);

        File pluginJar = new PluginJarBuilder("pluginType")
                .addPluginInformation("test.plugin", "foo", "1.0")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        BundleContext ctx = ((OsgiPlugin) pluginManager.getPlugin("test.plugin")).getBundle().getBundleContext();
        ServiceRegistration reg = ctx.registerService(ModuleDescriptor.class.getName(), new DummyWebItemModuleDescriptor(), null);

        File pluginJar2 = new PluginJarBuilder("pluginType")
                .addPluginInformation("test.plugin2", "foo", "1.0")
                .build();
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        BundleContext ctx2 = ((OsgiPlugin) pluginManager.getPlugin("test.plugin2")).getBundle().getBundleContext();
        ServiceRegistration reg2 = ctx2.registerService(ModuleDescriptor.class.getName(), new DummyWebItemModuleDescriptor(), null);

        Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
        ModuleDescriptor descriptor = descriptors.iterator().next();
        assertEquals("DummyWebItemModuleDescriptor", descriptor.getClass().getSimpleName());
        List<WebItemModuleDescriptor> list = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
        assertEquals(2, list.size());
        reg2.unregister();
        list = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
        assertEquals(1, list.size());
        descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
    }

    public void testPluginDependentOnPackageImport() throws Exception
    {
        PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        firstBuilder
                .addPluginInformation("first", "Some name", "1.0")
                .addFormattedJava("first.MyInterface",
                        "package first;",
                        "public interface MyInterface {}")
                .build(pluginsDir);

        new PluginJarBuilder("asecond", firstBuilder.getClassLoader())
                .addPluginInformation("second", "Some name", "1.0")
                .addFormattedJava("second.MyImpl",
                        "package second;",
                        "public class MyImpl implements first.MyInterface {}")
                .build(pluginsDir);

        initPluginManager();

        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertNotNull(pluginManager.getPlugin("first"));
        assertNotNull(pluginManager.getPlugin("second"));
    }

    public void testPluginWithServletDependentOnPackageImport() throws Exception
    {
        PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        firstBuilder
                .addPluginInformation("first", "Some name", "1.0")
                .addFormattedJava("first.MyInterface",
                        "package first;",
                        "public interface MyInterface {}")
                .build(pluginsDir);

        new PluginJarBuilder("asecond", firstBuilder.getClassLoader())
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='asecond' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <servlet key='foo' class='second.MyServlet'>",
                        "       <url-pattern>/foo</url-pattern>",
                        "    </servlet>",
                        "</atlassian-plugin>")
                .addFormattedJava("second.MyServlet",
                        "package second;",
                        "public class MyServlet extends javax.servlet.http.HttpServlet implements first.MyInterface {}")
                .build(pluginsDir);


        initPluginManager(null, new SingleModuleDescriptorFactory("servlet", StubServletModuleDescriptor.class));

        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertTrue(pluginManager.getPlugin("first").isEnabled());
        assertNotNull(pluginManager.getPlugin("asecond").isEnabled());
    }

    public void testLotsOfHostComponents() throws Exception
    {
        DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory();
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider(){
            public void provide(ComponentRegistrar registrar)
            {
                for (int x=0; x<500; x++)
                {
                    registrar.register(SomeInterface.class).forInstance(new SomeInterface(){}).withName("some"+x);
                    registrar.register(AnotherInterface.class).forInstance(new AnotherInterface(){}).withName("another"+x);
                }
            }
        }, factory);

        File pluginJar = new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "    <dummy key='dum1'/>",
                        "</atlassian-plugin>")
                .build();
        File pluginJar2 = new PluginJarBuilder("second")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "    <component-import key='comp2' interface='com.atlassian.plugin.osgi.AnotherInterface' />",
                        "    <dummy key='dum1'/>",
                        "    <dummy key='dum2'/>",
                        "</atlassian-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test", pluginManager.getPlugin("test.plugin").getName());
        assertEquals(1, pluginManager.getPlugin("test.plugin").getModuleDescriptors().size());
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals(2, pluginManager.getPlugin("test.plugin").getModuleDescriptors().size());
        assertEquals("Test 2", pluginManager.getPlugin("test.plugin").getName());
    }


    public static class DummyModuleDescriptor extends AbstractModuleDescriptor
    {
        public Object getModule()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }



}
