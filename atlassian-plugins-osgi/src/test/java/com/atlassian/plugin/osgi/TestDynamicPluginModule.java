package com.atlassian.plugin.osgi;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.atlassian.plugin.util.WaitUntil;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Bundle;

public class TestDynamicPluginModule extends PluginInContainerTestBase
{
    public void testDynamicPluginModule() throws Exception
    {
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
            }
        });

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='factory' class='foo.MyModuleDescriptorFactory' public='true'>",
                        "       <interface>com.atlassian.plugin.ModuleDescriptorFactory</interface>",
                        "    </component>",
                        "</atlassian-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends com.atlassian.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .addFormattedJava("foo.MyModuleDescriptorFactory",
                        "package foo;",
                        "public class MyModuleDescriptorFactory extends com.atlassian.plugin.DefaultModuleDescriptorFactory {",
                        "  public MyModuleDescriptorFactory() {",
                        "    super();",
                        "    addModuleDescriptor('foo', MyModuleDescriptor.class);",
                        "  }",
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
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator()
                .next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleNotLinkToAllPlugins() throws Exception
    {
        new PluginJarBuilder("pluginType")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor'/>",
                        "</atlassian-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends com.atlassian.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .build(pluginsDir);
        new PluginJarBuilder("fooUser")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</atlassian-plugin>")
                .build(pluginsDir);
        new PluginJarBuilder("foootherUser")
                .addPluginInformation("unusing.plugin", "Unusing plugin", "1.0")
                .build(pluginsDir);

        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
            }
        });

        assertEquals("MyModuleDescriptor", pluginManager.getPlugin("test.plugin").getModuleDescriptor("dum2").getClass().getSimpleName());
        Set<String> deps = findDependentBundles(((OsgiPlugin) pluginManager.getPlugin("test.plugin.module")).getBundle());
        assertTrue(deps.contains("test.plugin"));
        assertFalse(deps.contains("unusing.plugin"));
    }

    private Set<String> findDependentBundles(Bundle bundle)
    {
        Set<String> deps = new HashSet<String>();
        final ServiceReference[] registeredServices = bundle.getRegisteredServices();
        if (registeredServices == null)
        {
            return deps;
        }

        for (final ServiceReference serviceReference : registeredServices)
        {
            final Bundle[] usingBundles = serviceReference.getUsingBundles();
            if (usingBundles == null)
            {
                continue;
            }
            for (final Bundle usingBundle : usingBundles)
            {
                deps.add(usingBundle.getSymbolicName());
            }
        }
        return deps;
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptor() throws Exception
    {
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
            }
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
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        WaitUntil.invoke(new BasicWaitCondition()
        {
            public boolean isFinished()
            {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptor("dum2")
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });
        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator()
                .next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }


    public void testDynamicPluginModuleUsingModuleTypeDescriptorAndComponentInjection() throws Exception
    {
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
            }
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
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        WaitUntil.invoke(new BasicWaitCondition()
        {
            public boolean isFinished()
            {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptors()
                        .iterator()
                        .next()
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });
        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator()
                .next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorAfterTheFact() throws Exception
    {
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
            }
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
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        WaitUntil.invoke(new BasicWaitCondition()
        {
            public boolean isFinished()
            {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptors()
                        .iterator()
                        .next()
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });

        Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        ModuleDescriptor<?> descriptor = descriptors.iterator()
                .next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());

        pluginManager.uninstall(pluginManager.getPlugin("test.plugin.module"));
        WaitUntil.invoke(new BasicWaitCondition()
        {
            public boolean isFinished()
            {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptors()
                        .iterator()
                        .next()
                        .getClass()
                        .getSimpleName()
                        .equals("UnrecognisedModuleDescriptor");
            }
        });
        descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        descriptor = descriptors.iterator()
                .next();
        assertEquals("UnrecognisedModuleDescriptor", descriptor.getClass().getSimpleName());

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        descriptor = descriptors.iterator()
                .next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorAfterTheFactWithException() throws Exception
    {
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
            }
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
                        "  public MyModuleDescriptor() {",
                        "    throw new RuntimeException('error loading module');",
                        "  }",
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
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertTrue(WaitUntil.invoke(new BasicWaitCondition()
        {
            public boolean isFinished()
            {
                UnrecognisedModuleDescriptor des = (UnrecognisedModuleDescriptor) pluginManager.getPlugin("test.plugin").getModuleDescriptor("dum2");
                return des.getErrorText().contains("error loading module");
            }
        }));

    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorInSamePlugin() throws Exception
    {
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
            }
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
        WaitUntil.invoke(new BasicWaitCondition()
        {
            public boolean isFinished()
            {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptor("dum2")
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });
        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(2, descriptors.size());
        final ModuleDescriptor<?> descriptor = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptor("dum2");
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorInSamePluginWithRestart() throws Exception
    {
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
            }
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
        WaitUntil.invoke(new BasicWaitCondition()
        {
            public boolean isFinished()
            {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptor("dum2")
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });
        Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(2, descriptors.size());
        ModuleDescriptor<?> descriptor = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptor("dum2");
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        WaitUntil.invoke(new BasicWaitCondition()
        {
            public boolean isFinished()
            {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptor("dum2")
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });
        descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(2, descriptors.size());
        ModuleDescriptor<?> newdescriptor = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptor("dum2");
        assertEquals("MyModuleDescriptor", newdescriptor.getClass().getSimpleName());
        assertTrue(descriptor.getClass() != newdescriptor.getClass());
    }

    public void testDynamicModuleDescriptor() throws Exception
    {
        initPluginManager(null);

        final File pluginJar = new PluginJarBuilder("pluginType").addPluginInformation("test.plugin", "foo", "1.0")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        final BundleContext ctx = ((OsgiPlugin) pluginManager.getPlugin("test.plugin")).getBundle()
                .getBundleContext();
        final ServiceRegistration reg = ctx.registerService(ModuleDescriptor.class.getName(), new DummyWebItemModuleDescriptor(), null);

        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator()
                .next();
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

        final File pluginJar = new PluginJarBuilder("pluginType").addPluginInformation("test.plugin", "foo", "1.0")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        final BundleContext ctx = ((OsgiPlugin) pluginManager.getPlugin("test.plugin")).getBundle()
                .getBundleContext();
        ctx.registerService(ModuleDescriptor.class.getName(), new DummyWebItemModuleDescriptor(), null);

        final File pluginJar2 = new PluginJarBuilder("pluginType").addPluginInformation("test.plugin2", "foo", "1.0")
                .build();
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        final BundleContext ctx2 = ((OsgiPlugin) pluginManager.getPlugin("test.plugin2")).getBundle()
                .getBundleContext();
        final ServiceRegistration reg2 = ctx2.registerService(ModuleDescriptor.class.getName(), new DummyWebItemModuleDescriptor(), null);

        Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator()
                .next();
        assertEquals("DummyWebItemModuleDescriptor", descriptor.getClass().getSimpleName());
        List<WebItemModuleDescriptor> list = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
        assertEquals(2, list.size());
        reg2.unregister();
        list = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
        assertEquals(1, list.size());
        descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
    }
}
