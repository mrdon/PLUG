package com.atlassian.plugin.osgi;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginState;
import com.atlassian.plugin.util.WaitUntil;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.DefaultServletModuleManager;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import javax.servlet.ServletContext;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PluginInstallTest extends PluginInContainerTestBase
{

    public void testUpgradeWithNewComponentImports() throws Exception
    {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface()
                {});
                registrar.register(AnotherInterface.class).forInstance(new AnotherInterface()
                {});
            }
        }, factory);

        final File pluginJar = new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "    <dummy key='dum1'/>", "</atlassian-plugin>")
                .build();
        final File pluginJar2 = new PluginJarBuilder("second")
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
        assertEquals(2, pluginManager.getPlugin("test.plugin").getModuleDescriptors().size());
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals(4, pluginManager.getPlugin("test.plugin").getModuleDescriptors().size());
        assertEquals("Test 2", pluginManager.getPlugin("test.plugin").getName());
    }

    public void testUpgradeWithNoAutoDisable() throws Exception
    {
        DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
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
        final File pluginJar2 = new PluginJarBuilder("second")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "    <dummy key='dum1'/>",
                        "    <dummy key='dum2'/>",
                        "</atlassian-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertTrue(pluginManager.isPluginEnabled("test.plugin"));

        final Lock lock = new ReentrantLock();
        Thread upgradeThread = new Thread()
        {
            public void run()
            {
                lock.lock();
                pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
                lock.unlock();
            }
        };

        Thread isEnabledThread = new Thread()
        {
            public void run()
            {
                while (!lock.tryLock())
                    pluginManager.isPluginEnabled("test.plugin");
            }
        };
        upgradeThread.start();
        isEnabledThread.start();

        upgradeThread.join();

        assertTrue(pluginManager.isPluginEnabled("test.plugin"));
    }


    public void testUpgradeWithNewComponentImplementation() throws Exception
    {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface()
                {});
                registrar.register(AnotherInterface.class).forInstance(new AnotherInterface()
                {});
            }
        }, factory);

        final File pluginJar = new PluginJarBuilder("first")
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
        final File pluginJar2 = new PluginJarBuilder("second")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test 2' key='test2.plugin' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "    </plugin-info>",
                    "    <component-import key='svc' interface='java.util.concurrent.Callable' />",
                    "    <component key='del' class='my2.ServiceDelegate' public='true'>",
                    "    <interface>com.atlassian.plugin.osgi.Callable2</interface>",
                    "    </component>",
                    "</atlassian-plugin>")
                .addFormattedJava("my2.ServiceDelegate",
                    "package my2;",
                    "import com.atlassian.plugin.osgi.Callable2;",
                    "import java.util.concurrent.Callable;",
                    "public class ServiceDelegate implements Callable2 {",
                    "    private final Callable delegate;",
                    "    public ServiceDelegate(Callable foo) { this.delegate = foo;}",
                    "    public String call() throws Exception { return (String)delegate.call();}",
                    "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        final ServiceTracker tracker = osgiContainerManager.getServiceTracker("com.atlassian.plugin.osgi.Callable2");

        for (final Object svc : tracker.getServices())
        {
            final Callable2 callable = (Callable2) svc;
            assertEquals("hi", callable.call());
        }
        assertEquals(2, pluginManager.getEnabledPlugins().size());

        final File updatedJar = new PluginJarBuilder("first")
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
        WaitUntil.invoke(new AbstractWaitCondition()
        {
            public boolean isFinished()
            {
                return pluginManager.getEnabledPlugins().size() == 2;
            }
        });
        assertEquals(2, pluginManager.getEnabledPlugins().size());
        for (final Object svc : tracker.getServices())
        {
            final Callable2 callable = (Callable2) svc;
            assertEquals("bob", callable.call());
        }
    }

    public void testUpgradeWithNewComponentImplementationWithInterfaceInPlugin() throws Exception
    {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface()
                {});
                registrar.register(AnotherInterface.class).forInstance(new AnotherInterface()
                {});
            }
        }, factory);

        final PluginJarBuilder builder1 = new PluginJarBuilder("first")
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
        final File pluginJar = builder1.build();
        final File pluginJar2 = new PluginJarBuilder("second", builder1.getClassLoader())
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
        final ServiceTracker tracker = osgiContainerManager.getServiceTracker(Callable.class.getName());
        final ServiceTracker svcTracker = osgiContainerManager.getServiceTracker("my.Service");

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        assertEquals("hi", svcTracker.getService().getClass().getMethod("call").invoke(svcTracker.getService()));
        assertEquals("hi", ((Callable) tracker.getService()).call());

        assertEquals(2, pluginManager.getEnabledPlugins().size());

        final File updatedJar = new PluginJarBuilder("first")
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
        assertEquals("bob", svcTracker.getService().getClass().getMethod("call").invoke(svcTracker.getService()));
        tracker.waitForService(5000);
        assertEquals("bob", ((Callable) tracker.getService()).call());
    }

    public void testUpgradeWithRefreshingAffectingOtherPlugins() throws Exception
    {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
            }
        }, factory);

        PluginJarBuilder pluginBuilder = new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions><Export-Package>my</Export-Package></bundle-instructions>",
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
                    "}");
        final File pluginJar = pluginBuilder.build();

        final File pluginJar2 = new PluginJarBuilder("second", pluginBuilder.getClassLoader())
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test 2' key='test2.plugin' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions><Import-Package>my,*</Import-Package></bundle-instructions>",
                    "    </plugin-info>",
                    "    <component-import key='svc' interface='java.util.concurrent.Callable' />",
                    "    <component-import key='othersvc' interface='com.atlassian.plugin.osgi.Callable3' />",
                    "    <component key='del' class='my2.ServiceDelegate' public='true'>",
                    "    <interface>com.atlassian.plugin.osgi.Callable2</interface>",
                    "    </component>",
                    "</atlassian-plugin>")
                .addFormattedJava("my2.ServiceDelegate",
                    "package my2;",
                    "import com.atlassian.plugin.osgi.Callable2;",
                    "import com.atlassian.plugin.osgi.Callable3;",
                    "import java.util.concurrent.Callable;",
                    "public class ServiceDelegate implements Callable2 {",
                    "    private final Callable delegate;",
                    "    private final Callable3 othersvc;",
                    "    public ServiceDelegate(Callable foo,Callable3 othersvc) {",
                    "        this.delegate = foo;",
                    "        this.othersvc = othersvc;",
                    "    }",
                    "    public String call() throws Exception { return othersvc.call() + (String)delegate.call();}",
                    "}")
                .build();
        final File otherSvcJar = new PluginJarBuilder("otherSvc")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='test.othersvc.plugin' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "    </plugin-info>",
                    "    <component key='othersvc' class='othersvc.ServiceImpl' public='true'>",
                    "    <interface>com.atlassian.plugin.osgi.Callable3</interface>",
                    "    </component>",
                    "</atlassian-plugin>")
                .addFormattedJava("othersvc.ServiceImpl",
                    "package othersvc;",
                    "import com.atlassian.plugin.osgi.Callable3;",
                    "public class ServiceImpl implements Callable3 {",
                    "    public String call() throws Exception { return 'hi';}",
                    "}")
                .build();


        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());

        pluginManager.installPlugin(new JarPluginArtifact(otherSvcJar));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));

        assertEquals("hi", ((OsgiPlugin)pluginManager.getPlugin("test2.plugin")).autowire(Callable3Aware.class).call());
        assertEquals(3, pluginManager.getEnabledPlugins().size());

        final File updatedJar = new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions><Export-Package>my</Export-Package></bundle-instructions>",
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
        assertEquals(3, pluginManager.getEnabledPlugins().size());
        assertEquals("hi", ((OsgiPlugin)pluginManager.getPlugin("test2.plugin")).autowire(Callable3Aware.class).call());
    }



    public void testUpgradeTestingForCachedXml() throws Exception
    {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface()
                {});
                registrar.register(AnotherInterface.class).forInstance(new AnotherInterface()
                {});
            }
        }, factory);

        final File pluginJar = new PluginJarBuilder("first").addFormattedResource("atlassian-plugin.xml",
            "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>", "    <plugin-info>", "        <version>1.0</version>",
            "    </plugin-info>", "    <component key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' class='my.ServiceImpl' />",
            "</atlassian-plugin>").addFormattedJava("my.ServiceImpl", "package my;",
            "public class ServiceImpl implements com.atlassian.plugin.osgi.SomeInterface {}").build();
        final File pluginJar2 = new PluginJarBuilder("second").addFormattedResource("atlassian-plugin.xml",
            "<atlassian-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>", "    <plugin-info>", "        <version>1.0</version>",
            "    </plugin-info>", "</atlassian-plugin>").build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test", pluginManager.getPlugin("test.plugin").getName());
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test 2", pluginManager.getPlugin("test.plugin").getName());
    }

    public void testDynamicPluginModule() throws Exception
    {
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {}
        });

        final File pluginJar = new PluginJarBuilder("pluginType").addFormattedResource("atlassian-plugin.xml",
            "<atlassian-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>", "    <plugin-info>", "        <version>1.0</version>",
            "    </plugin-info>", "    <component key='factory' class='foo.MyModuleDescriptorFactory' public='true'>",
            "       <interface>com.atlassian.plugin.ModuleDescriptorFactory</interface>", "    </component>", "</atlassian-plugin>").addJava(
            "foo.MyModuleDescriptor",
            "package foo;" + "public class MyModuleDescriptor extends com.atlassian.plugin.descriptors.AbstractModuleDescriptor {" + "  public Object getModule(){return null;}" + "}").addJava(
            "foo.MyModuleDescriptorFactory",
            "package foo;" + "public class MyModuleDescriptorFactory extends com.atlassian.plugin.DefaultModuleDescriptorFactory {" + "  public MyModuleDescriptorFactory() {" + "    super();" + "    addModuleDescriptor(\"foo\", MyModuleDescriptor.class);" + "  }" + "}").build();
        final File pluginJar2 = new PluginJarBuilder("fooUser").addFormattedResource("atlassian-plugin.xml",
            "<atlassian-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>", "    <plugin-info>", "        <version>1.0</version>",
            "    </plugin-info>", "    <foo key='dum2'/>", "</atlassian-plugin>").build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator().next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptor() throws Exception
    {
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {}
        });

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "</atlassian-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends com.atlassian.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .build();
        final File pluginJar2 = new PluginJarBuilder("fooUser")
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
        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator().next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorAndComponentInjection() throws Exception
    {
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {}
        });

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='comp' class='foo.MyComponent' />",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "</atlassian-plugin>")
                .addFormattedJava("foo.MyComponent",
                        "package foo;",
                        "public class MyComponent {",
                        "}")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends com.atlassian.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public MyModuleDescriptor(MyComponent comp) {}",
                        "  public Object getModule(){return null;}",
                        "}")

                .build();
        final File pluginJar2 = new PluginJarBuilder("fooUser")
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
        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator().next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorAfterTheFact() throws Exception
    {
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {}
        });

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "</atlassian-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends com.atlassian.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .build();
        final File pluginJar2 = new PluginJarBuilder("fooUser")
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
        ModuleDescriptor<?> descriptor = descriptors.iterator().next();
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
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {}
        });

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "    <foo key='dum2' />",
                        "</atlassian-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends com.atlassian.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        Thread.sleep(5000);
        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(2, descriptors.size());
        final ModuleDescriptor<?> descriptor = pluginManager.getPlugin("test.plugin").getModuleDescriptor("dum2");
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicModuleDescriptor() throws Exception
    {
        initPluginManager(null);

        final File pluginJar = new PluginJarBuilder("pluginType").addPluginInformation("test.plugin", "foo", "1.0").build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        final BundleContext ctx = ((OsgiPlugin) pluginManager.getPlugin("test.plugin")).getBundle().getBundleContext();
        final ServiceRegistration reg = ctx.registerService(ModuleDescriptor.class.getName(), new DummyWebItemModuleDescriptor(), null);

        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator().next();
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

        final File pluginJar = new PluginJarBuilder("pluginType").addPluginInformation("test.plugin", "foo", "1.0").build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        final BundleContext ctx = ((OsgiPlugin) pluginManager.getPlugin("test.plugin")).getBundle().getBundleContext();
        ctx.registerService(ModuleDescriptor.class.getName(), new DummyWebItemModuleDescriptor(), null);

        final File pluginJar2 = new PluginJarBuilder("pluginType").addPluginInformation("test.plugin2", "foo", "1.0").build();
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        final BundleContext ctx2 = ((OsgiPlugin) pluginManager.getPlugin("test.plugin2")).getBundle().getBundleContext();
        final ServiceRegistration reg2 = ctx2.registerService(ModuleDescriptor.class.getName(), new DummyWebItemModuleDescriptor(), null);

        Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator().next();
        assertEquals("DummyWebItemModuleDescriptor", descriptor.getClass().getSimpleName());
        List<WebItemModuleDescriptor> list = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
        assertEquals(2, list.size());
        reg2.unregister();
        list = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
        assertEquals(1, list.size());
        descriptors = pluginManager.getPlugin("test.plugin").getModuleDescriptors();
        assertEquals(1, descriptors.size());
    }

    public void testPluginDependentOnPackageImporttestPluginWithHostComponentUsingOldPackageImport() throws Exception
    {
        HostComponentProvider prov = new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                registrar.register(ServletConfig.class).forInstance(new HttpServlet() {});
            }
        };
        File servletJar =  new PluginJarBuilder("first")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Export-Package: javax.servlet.http;version='4.0.0',javax.servlet;version='4.0.0'",
                        "Import-Package: javax.servlet.http;version='4.0.0',javax.servlet;version='4.0.0'",
                        "Bundle-SymbolicName: first",
                        "Bundle-Version: 4.0.0",
                        "Manifest-Version: 1.0",
                        "")
                .addFormattedJava("javax.servlet.Servlet",
                        "package javax.servlet;",
                        "public interface Servlet {}")
                .addFormattedJava("javax.servlet.http.HttpServlet",
                        "package javax.servlet.http;",
                        "public abstract class HttpServlet implements javax.servlet.Servlet{}")
                .build();

        File pluginJar = new PluginJarBuilder("asecond")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='second' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions><Import-Package>javax.servlet.http;version='[2.3,2.3]',javax.servlet;version='[2.3,2.3]',*</Import-Package></bundle-instructions>",
                    "    </plugin-info>",
                    "</atlassian-plugin>")
                .addFormattedJava("second.MyImpl",
                        "package second;",
                        "public class MyImpl {",
                        "    public MyImpl(javax.servlet.ServletConfig config) {",
                        "    }",
                        "}")
                .build();

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(servletJar));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));

        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertNotNull(pluginManager.getPlugin("first-4.0.0"));
        assertNotNull(pluginManager.getPlugin("second"));
    }

    public void testPluginWithHostComponentUsingOldPackageImport() throws Exception
    {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        firstBuilder
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='first' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions>",
                    "           <Export-Package>first</Export-Package>",
                    "        </bundle-instructions>",
                    "    </plugin-info>",
                    "    <servlet key='foo' class='second.MyServlet'>",
                    "       <url-pattern>/foo</url-pattern>",
                    "    </servlet>",
                    "</atlassian-plugin>")
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
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        firstBuilder
                .addPluginInformation("first", "Some name", "1.0")
                .addFormattedJava("first.MyInterface",
                        "package first;",
                        "public interface MyInterface {}")
                .addFormattedResource("META-INF/MANIFEST.MF",
                    "Manifest-Version: 1.0",
                    "Bundle-SymbolicName: first",
                    "Export-Package: first",
                    "")
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

        initPluginManager(null, new SingleModuleDescriptorFactory(new DefaultHostContainer(), "servlet", StubServletModuleDescriptor.class));

        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertTrue(pluginManager.getPlugin("first").getPluginState() == PluginState.ENABLED);
        assertNotNull(pluginManager.getPlugin("asecond").getPluginState() == PluginState.ENABLED);
    }

    public void testPluginWithServletRefreshedAfterOtherPluginUpgraded() throws Exception
    {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        firstBuilder
                .addPluginInformation("first", "Some name", "1.0")
                .addFormattedJava("first.MyInterface",
                        "package first;",
                        "public interface MyInterface {}")
                .addFormattedResource("META-INF/MANIFEST.MF",
                    "Manifest-Version: 1.0",
                    "Bundle-SymbolicName: first",
                    "Export-Package: first",
                    "")
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
                    "import com.atlassian.plugin.osgi.Callable2;",
                    "public class MyServlet extends javax.servlet.http.HttpServlet implements first.MyInterface {",
                    "   private Callable2 callable;",
                    "   public MyServlet(Callable2 cal) { this.callable = cal; }",
                    "   public String getServletInfo() {",
                    "       try {return callable.call() + ' bob';} catch (Exception ex) { throw new RuntimeException(ex);}",
                    "   }",
                    "}")
                .build(pluginsDir);

        HostComponentProvider prov = new HostComponentProvider()
        {

            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(Callable2.class).forInstance(new Callable2()
                {
                    public String call()
                    {
                        return "hi";
                    }
                });

            }
        };

        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        mockServletContext.expect("log", C.ANY_ARGS);
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockServletConfig = new Mock(ServletConfig.class);
        mockServletConfig.matchAndReturn("getServletContext", mockServletContext.proxy());

        ServletModuleManager mgr = new DefaultServletModuleManager(pluginEventManager);
        Mock mockHostContainer = new Mock(HostContainer.class);
        mockHostContainer.matchAndReturn("create", C.args(C.eq(ServletModuleDescriptor.class)), new StubServletModuleDescriptor(mgr));
        initPluginManager(prov, new SingleModuleDescriptorFactory(
                (HostContainer) mockHostContainer.proxy(),
                "servlet",
                ServletModuleDescriptor.class));

        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertTrue(pluginManager.getPlugin("first").getPluginState() == PluginState.ENABLED);
        assertNotNull(pluginManager.getPlugin("asecond").getPluginState() == PluginState.ENABLED);
        assertEquals("hi bob", mgr.getServlet("/foo", (ServletConfig) mockServletConfig.proxy()).getServletInfo());



        final File updatedJar = new PluginJarBuilder("first-updated")
                .addPluginInformation("foo", "Some name", "1.0")
                .addFormattedJava("first.MyInterface",
                        "package first;",
                        "public interface MyInterface {}")
                .addFormattedResource("META-INF/MANIFEST.MF",
                    "Manifest-Version: 1.0",
                    "Bundle-SymbolicName: foo",
                    "Export-Package: first",
                    "")
                .build();
        pluginManager.installPlugin(new JarPluginArtifact(updatedJar));

        Thread.sleep(5000);
        WaitUntil.invoke(new WaitUntil.WaitCondition()
        {
            public boolean isFinished()
            {
                return pluginManager.isPluginEnabled("asecond");
            }

            public String getWaitMessage()
            {
                return null;
            }
        }, 10, TimeUnit.SECONDS, 2);

        assertEquals("hi bob", mgr.getServlet("/foo", (ServletConfig) mockServletConfig.proxy()).getServletInfo());
    }

    public void testLotsOfHostComponents() throws Exception
    {
        new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <dummy key='dum1'/>",
                        "</atlassian-plugin>")
                .build(pluginsDir);
        new PluginJarBuilder("second")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test.plugin2' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <dummy key='dum1'/>",
                        "    <dummy key='dum2'/>",
                        "</atlassian-plugin>")
                .build(pluginsDir);

        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                for (int x = 0; x < 100; x++)
                {
                    registrar.register(SomeInterface.class).forInstance(new SomeInterface()
                    {}).withName("some" + x);
                    registrar.register(AnotherInterface.class).forInstance(new AnotherInterface()
                    {}).withName("another" + x);
                }
            }
        }, factory);

        assertEquals(2, pluginManager.getEnabledPlugins().size());
    }

    public void testInstallWithStrangePath() throws Exception
    {
        File strangeDir = new File(tmpDir, "20%time");
        strangeDir.mkdir();
        File oldTmp = tmpDir;
        try
        {
            tmpDir = strangeDir;
            cacheDir = new File(tmpDir, "felix-cache");
            cacheDir.mkdir();
            pluginsDir = new File(tmpDir, "plugins");
            pluginsDir.mkdir();


            new PluginJarBuilder("strangePath")
                    .addFormattedResource("atlassian-plugin.xml",
                            "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                            "    <plugin-info>",
                            "        <version>1.0</version>",
                            "    </plugin-info>",
                            "</atlassian-plugin>")
                    .build(pluginsDir);

            final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
            factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
            initPluginManager(new HostComponentProvider()
            {
                public void provide(final ComponentRegistrar registrar)
                {
                }
            }, factory);

            assertEquals(1, pluginManager.getEnabledPlugins().size());
        }
        finally
        {
            tmpDir = oldTmp;
        }
    }

    public static class Callable3Aware
    {
        private final Callable3 callable;

        public Callable3Aware(Callable3 callable)
        {
            this.callable = callable;
        }

        public String call() throws Exception
        {
            return callable.call();
        }
    }
}
