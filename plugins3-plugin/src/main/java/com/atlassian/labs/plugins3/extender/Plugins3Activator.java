package com.atlassian.labs.plugins3.extender;

import com.atlassian.labs.plugins3.api.PluginDescriptor;
import com.atlassian.labs.plugins3.api.PluginDescriptorGenerator;
import com.atlassian.labs.plugins3.spi.PluginDescriptorGeneratorFactory;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.factory.OsgiPluginXmlDescriptorParserFactory;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.parsers.XmlDescriptorParserFactory;
import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.SynchronousBundleListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 */
public class Plugins3Activator implements BundleActivator
{
    private Set<Bundle> processBundles = new CopyOnWriteArraySet<Bundle>();
    private BundleListener bundleListener;

    public void start(final BundleContext bundleContext) throws Exception
    {
        // todo: get from osgi
        final PluginDescriptorGeneratorFactory pluginDescriptorGeneratorFactory = new DummyPluginDescriptorGeneratorFactory(bundleContext);
        final PluginAccessor pluginAccessor = (PluginAccessor) bundleContext.getService(bundleContext.getServiceReference(PluginAccessor.class.getName())); // get plugin accessor

        for (Bundle bundle : bundleContext.getBundles())
        {
            if (bundle.getState() == Bundle.ACTIVE || bundle.getState() == Bundle.RESOLVED)
            {
                addBundle(bundle, pluginAccessor, pluginDescriptorGeneratorFactory, bundleContext);
            }
        }
        bundleListener = new SynchronousBundleListener()
        {

            public void bundleChanged(BundleEvent event)
            {
                switch (event.getType())
                {
                    case BundleEvent.RESOLVED : addBundle(event.getBundle(), pluginAccessor, pluginDescriptorGeneratorFactory, bundleContext);
                                                break;
                    case BundleEvent.UNINSTALLED : removeBundle(event.getBundle());

                }
            }
        };
        bundleContext.addBundleListener(bundleListener);
    }

    private void removeBundle(Bundle bundle)
    {
        processBundles.remove(bundle);
    }

    private void addBundle(Bundle bundle, PluginAccessor pluginAccessor, PluginDescriptorGeneratorFactory pluginDescriptorGeneratorFactory, BundleContext bundleContext)
    {
        if (processBundles.contains(bundle))
        {
            return;
        }
        try
        {
            Class<PluginDescriptor> clazz = bundle.loadClass("AtlassianPlugin");
            PluginDescriptor pluginConfiguration = clazz.newInstance();
            String pluginKey = OsgiHeaderUtil.getPluginKey(bundle);
            Plugin plugin = pluginAccessor.getPlugin(pluginKey);
            Document doc = DocumentFactory.getInstance().createDocument();
            Element root = doc.addElement("atlassian-plugin");
            root.addAttribute("key", pluginKey);
            PluginDescriptorGenerator descriptor = pluginDescriptorGeneratorFactory.newInstance(doc);
            pluginConfiguration.config(pluginDescriptorGeneratorFactory.getApplicationInfo(), descriptor);


            apply(doc, plugin, pluginDescriptorGeneratorFactory.getModuleDescriptorFactory(plugin));
            processBundles.add(bundle);
        }
        catch (ClassNotFoundException e)
        {
            // ignore
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (Exception e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void apply(Document doc, Plugin plugin, ModuleDescriptorFactory moduleDescriptorFactory)
    {
        OsgiPluginXmlDescriptorParserFactory parserFactory = new OsgiPluginXmlDescriptorParserFactory();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLWriter xmlWriter = null;
        try
        {
            xmlWriter = new XMLWriter(out);
            xmlWriter.write(doc);
            System.out.println("Created plugin xml:" + new String(out.toByteArray()));
            DescriptorParser parser = parserFactory.getInstance(new ByteArrayInputStream(out.toByteArray()));
            parser.configurePlugin(moduleDescriptorFactory, plugin);
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (IOException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
        if (bundleListener != null)
        {
            bundleContext.removeBundleListener(bundleListener);
        }

    }
}
