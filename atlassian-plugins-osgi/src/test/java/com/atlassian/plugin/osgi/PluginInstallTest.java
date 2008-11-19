package com.atlassian.plugin.osgi;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;

import java.io.File;
import java.util.List;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

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


    public void testUpgradeWithNewComponentImplementation() throws Exception
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
                        "    <component key='svc' class='my.ServiceImpl' public='true'>",
                        "    <interface>java.util.concurrent.Callable</interface>",
                        "    </component>",
                        "</atlassian-plugin>")
                .addFormattedJava("my.ServiceImpl",
                        "package my;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceImpl implements Callable {",
                        "    public Object call() throws Exception { return 'hi';}",
                        "}")
                .build();
        File pluginJar2 = new PluginJarBuilder("second")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test2.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='svc' interface='java.util.concurrent.Callable' />",
                        "    <component key='del' class='my2.ServiceDelegate'>",
                        "    <interface>java.util.concurrent.Callable</interface>",
                        "    </component>",
                        "</atlassian-plugin>")
                .addFormattedJava("my2.ServiceDelegate",
                        "package my2;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceDelegate implements Callable {",
                        "    private final Callable delegate;",
                        "    public ServiceDelegate(Callable foo) { this.delegate = foo;}",
                        "    public Object call() throws Exception { return delegate.call();}",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        ServiceTracker tracker = osgiContainerManager.getServiceTracker("java.util.concurrent.Callable");
        for (Object svc : tracker.getServices())
        {
            Callable callable = (Callable) svc;
            assertEquals("hi", callable.call());
        }

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));

        for (Object svc : tracker.getServices())
        {
            Callable callable = (Callable) svc;
            assertEquals("hi", callable.call());
        }
        assertEquals(2, pluginManager.getEnabledPlugins().size());

        File updatedJar = new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='svc' class='my.ServiceImpl' public='true'>",
                        "    <interface>java.util.concurrent.Callable</interface>",
                        "    </component>",
                        "</atlassian-plugin>")
                .addFormattedJava("my.ServiceImpl",
                        "package my;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceImpl implements Callable {",
                        "    public Object call() throws Exception { return 'bob';}",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(updatedJar));
        assertEquals(2, pluginManager.getEnabledPlugins().size());
        for (Object svc : tracker.getServices())
        {
            Callable callable = (Callable) svc;
            assertEquals("bob", callable.call());
        }
    }

    public void testUpgradeWithNewComponentImplementationWithInterfaceInPlugin() throws Exception
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

        PluginJarBuilder builder1  = new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='svc' class='my.ServiceImpl' public='true'>",
                        "    <interface>my.Service</interface>",
                        "    </component>",
                        "</atlassian-plugin>")
                .addFormattedJava("my.Service",
                        "package my;",
                        "public interface Service {",
                        "    public Object call() throws Exception;",
                        "}")
                .addFormattedJava("my.ServiceImpl",
                        "package my;",
                        "public class ServiceImpl implements Service {",
                        "    public Object call() throws Exception { return 'hi';}",
                        "}");
        File pluginJar = builder1.build();
        File pluginJar2 = new PluginJarBuilder("second", builder1.getClassLoader())
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test2.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='svc' interface='my.Service' />",
                        "    <component key='del' class='my2.ServiceDelegate' public='true'>",
                        "    <interface>java.util.concurrent.Callable</interface>",
                        "    </component>",
                        "</atlassian-plugin>")
                .addFormattedJava("my2.ServiceDelegate",
                        "package my2;",
                        "import my.Service;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceDelegate implements Callable {",
                        "    private final Service delegate;",
                        "    public ServiceDelegate(Service foo) { this.delegate = foo;}",
                        "    public Object call() throws Exception { return delegate.call();}",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        ServiceTracker tracker = osgiContainerManager.getServiceTracker(Callable.class.getName());
        ServiceTracker svcTracker = osgiContainerManager.getServiceTracker("my.Service");

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        assertEquals("hi", svcTracker.getService().getClass().getMethod("call").invoke(svcTracker.getService()));
        assertEquals("hi", ((Callable)tracker.getService()).call());

        assertEquals(2, pluginManager.getEnabledPlugins().size());

        File updatedJar = new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='svc' class='my.Service2Impl' public='true'>",
                        "    <interface>my.Service</interface>",
                        "    </component>",
                        "</atlassian-plugin>")
                .addFormattedJava("my.Service",
                        "package my;",
                        "public interface Service {",
                        "    public Object call() throws Exception;",
                        "}")
                .addFormattedJava("my.Service2Impl",
                        "package my;",
                        "public class Service2Impl implements Service {",
                        "    public Object call() throws Exception {return 'bob';}",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(updatedJar));
        svcTracker.waitForService(5000);
        assertEquals("bob", svcTracker.getService().getClass().getMethod("call").invoke(svcTracker.getService()));
        assertEquals("bob", ((Callable)tracker.getService()).call());
    }


    public void testUpgradeTestingForCachedXml() throws Exception
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
                        "    <component key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' class='my.ServiceImpl' />",
                        "</atlassian-plugin>")
                .addFormattedJava("my.ServiceImpl",
                        "package my;",
                        "public class ServiceImpl implements com.atlassian.plugin.osgi.SomeInterface {}")
                .build();
        File pluginJar2 = new PluginJarBuilder("second")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "</atlassian-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test", pluginManager.getPlugin("test.plugin").getName());
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
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

    public void testDynamicPluginModuleUsingModuleTypeDescriptorAfterTheFact() throws Exception
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

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        Thread.sleep(5000);
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
        ModuleDescriptor descriptor = descriptors.iterator().next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());

        pluginManager.uninstall(pluginManager.getPlugin("test.plugin.module"));
        Thread.sleep(5000);
        descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
        descriptor = descriptors.iterator().next();
        assertEquals("UnrecognisedModuleDescriptor", descriptor.getClass().getSimpleName());


        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
        descriptor = descriptors.iterator().next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorInSamePlugin() throws Exception
    {
        initPluginManager(new HostComponentProvider(){public void provide(ComponentRegistrar registrar){}});

        File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "    <foo key='dum2' />",
                        "</atlassian-plugin>")
                .addJava("foo.MyModuleDescriptor",
                        "package foo;" +
                        "public class MyModuleDescriptor extends com.atlassian.plugin.descriptors.AbstractModuleDescriptor {" +
                        "  public Object getModule(){return null;}" +
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        Thread.sleep(5000);
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
        File pluginJar = new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <dummy key='dum1'/>",
                        "</atlassian-plugin>")
                .build(pluginsDir);
        File pluginJar2 = new PluginJarBuilder("second")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test.plugin2' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <dummy key='dum1'/>",
                        "    <dummy key='dum2'/>",
                        "</atlassian-plugin>")
                .build(pluginsDir);

        DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory();
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider(){
            public void provide(ComponentRegistrar registrar)
            {
                for (int x=0; x<100; x++)
                {
                    registrar.register(SomeInterface.class).forInstance(new SomeInterface(){}).withName("some"+x);
                    registrar.register(AnotherInterface.class).forInstance(new AnotherInterface(){}).withName("another"+x);
                }
            }
        }, factory);

        assertEquals(2, pluginManager.getEnabledPlugins().size());
    }
}
