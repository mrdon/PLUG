package com.atlassian.plugin.osgi;

import com.atlassian.plugin.test.PluginBuilder;
import com.atlassian.plugin.PluginJar;
import com.atlassian.plugin.FilePluginJar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: 10/07/2008
 * Time: 10:50:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class ContextClassLoaderTest extends PluginInContainerTestBase {

    public void testCorrectContextClassLoaderForHostComponents() throws Exception
    {
        final HostComp comp = new HostComp();
        File plugin = new PluginBuilder("ccltest")
                .addPluginInformation("ccltest.key", "CCLTest", "1.0")
                .addJava("my.Foo", "package my;import com.atlassian.plugin.osgi.ContextClassLoaderTest.HostComp;public class Foo {public Foo(HostComp comp) { comp.run(); } }")
                .build();
        HostComponentProvider prov = new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(HostComp.class).forInstance(comp);
            }
        };

        initPluginManager(prov);
        //pluginManager.installPlugin(new FilePluginJar(plugin));

        //assertNotNull(comp.cl);

    }

    public static class HostComp
    {
        public ClassLoader cl;
        public void run()
        {
            cl = Thread.currentThread().getContextClassLoader();
        }
    }
}
