package com.atlassian.plugin.osgi.factory;

import junit.framework.TestCase;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.PluginState;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginContainerRefreshedEvent;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import org.osgi.framework.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Dictionary;
import java.util.Hashtable;

public class TestOsgiPlugin extends TestCase
{
    Mock mockBundle;
    OsgiPlugin plugin;
    Mock mockBundleContext;

    @Override
    public void setUp()
    {

        mockBundle = new Mock(Bundle.class);
        Dictionary dict = new Hashtable();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        mockBundle.matchAndReturn("getHeaders", dict);
        mockBundleContext = new Mock(BundleContext.class);

        plugin = new OsgiPlugin((Bundle) mockBundle.proxy(), new DefaultPluginEventManager());
    }

    @Override
    public void tearDown()
    {
        mockBundle = null;
        plugin = null;
        mockBundleContext = null;
    }
    public void testEnabled() {
        mockBundle.expectAndReturn("getBundleContext", null);
        mockBundle.expectAndReturn("getState", Bundle.RESOLVED);
        mockBundle.expect("start");
        plugin.enable();
        mockBundle.verify();
    }
    public void testDisabled() {
        mockBundle.expectAndReturn("getState", Bundle.ACTIVE);
        mockBundle.expect("stop");
        plugin.disable();
        mockBundle.verify();
    }

    public void testClose() {
        mockBundle.expectAndReturn("getState", Bundle.ACTIVE);
        mockBundle.expect("uninstall");
        plugin.close();
        mockBundle.verify();
    }

    public void testisEnabled() {
        mockBundle.expectAndReturn("getState", Bundle.ACTIVE);
        assertTrue(plugin.isEnabled());
        mockBundle.verify();

        mockBundle.expectAndReturn("getState", Bundle.RESOLVED);
        assertTrue(!plugin.isEnabled());
        mockBundle.verify();
    }

    public void testAutowireObject() {
        StaticListableBeanFactory bf = new StaticListableBeanFactory();
        bf.addBean("child", new ChildBean());
        DefaultListableBeanFactory autowireBf = new DefaultListableBeanFactory(bf);

        Mock mockBundle = new Mock(Bundle.class);
        Mock mockBundleContext = new Mock(BundleContext.class);
        mockBundle.expectAndReturn("getBundleContext", mockBundleContext.proxy());

        OsgiPlugin plugin = new OsgiPlugin((Bundle) mockBundle.proxy(), new DefaultPluginEventManager());
        plugin.setKey("foo");
        plugin.onSpringContextRefresh(new PluginContainerRefreshedEvent(new GenericApplicationContext(autowireBf), "foo"));
        SetterInjectedBean bean = new SetterInjectedBean();
        plugin.autowire(bean, AutowireCapablePlugin.AutowireStrategy.AUTOWIRE_BY_NAME);
        assertNotNull(bean.getChild());
        mockBundle.verify();
        mockBundleContext.verify();

    }

    public static class SetterInjectedBean
    {
        private ChildBean child;

        public ChildBean getChild()
        {
            return child;
        }

        public void setChild(ChildBean child)
        {
            this.child = child;
        }
    }

    public static class ChildBean {
    }

}