package com.atlassian.plugin.osgi.factory;

import junit.framework.TestCase;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import com.atlassian.plugin.AutowireCapablePlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
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

    @Override
    public void setUp()
    {
        mockBundle = new Mock(Bundle.class);
        Dictionary dict = new Hashtable();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        mockBundle.matchAndReturn("getHeaders", dict);

        plugin = new OsgiPlugin((Bundle) mockBundle.proxy());
    }

    @Override
    public void tearDown()
    {
        mockBundle = null;
        plugin = null;
    }
    public void testEnabled() {
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
        mockBundleContext.expectAndReturn("getServiceReferences", C.ANY_ARGS, new ServiceReference[1]);
        mockBundleContext.expectAndReturn("getService", C.ANY_ARGS, new GenericApplicationContext(autowireBf));
        mockBundle.expectAndReturn("getBundleContext", mockBundleContext.proxy());
        mockBundle.expectAndReturn("getSymbolicName", "foo");

        OsgiPlugin plugin = new OsgiPlugin((Bundle) mockBundle.proxy());
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