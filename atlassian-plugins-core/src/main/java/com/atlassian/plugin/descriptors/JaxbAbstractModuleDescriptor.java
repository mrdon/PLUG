package com.atlassian.plugin.descriptors;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXB;
import javax.xml.bind.util.JAXBResult;

import org.dom4j.Element;

import sun.security.action.GetLongAction;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.module.ModuleFactory;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Convenience class to create a plugin point:
 * <ul>
 * As a plugin point provider, this is your check-list:
 * <li>Extend this class.</li>
 * <li>Declare it in atlassian-plugin.xml: {@literal <module-type key="my-plugin-point" class="org.example.TestPluginPoint"  />}</li>
 * <li>If you want to use this plugin point outside of your project, make sure you tell AMPS to export the class (<Export-Package> in the pom.xml)
 * </ul>
 * <ul>
 * The user will:
 * <li>Use it in atlassian-plugin.xml: {@literal <my-plugin-point key="book1"> <title>That's my book</title> </my-plugin-point>}</li>
 * </ul>
 * 
 * <ul>
 * The consumer will:
 * <li>Retrieve the instances using {@link #getInstances(Class, PluginAccessor)} or the methods of {@link PluginAccessor}.</li>
 * </ul>
 * 
 * @param <T>
 *            the JAXB class that you want to read your configuration into
 * @param <M> the module class. You may use Void.
 * @since 2.11
 * 
 * 
 */
public abstract class JaxbAbstractModuleDescriptor<T extends JaxbAbstractModuleDescriptor.Bean, M> extends AbstractModuleDescriptor<M>
{
    /** The Jaxb bean into which the configuration is parsed */
    protected T configuration;
    private final Class<T> jaxbClass;

    public JaxbAbstractModuleDescriptor(ModuleFactory moduleFactory, Class<T> jaxbClass)
    {
        super(moduleFactory);
        this.jaxbClass = jaxbClass;
    }

    /**
     * Method called when the configuration is set. The plugin point developer can add some
     * initialisation.
     * 
     * @param plugin
     *            the plugin which generated this instance
     * @param configuration
     *            the configuration
     */
    public abstract void init(Plugin plugin, T configuration);

    /**
     * Implements the module by transforming 'element' into 'configuration' using JAXB
     */
    @Override
    final public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);

        try
        {
            String elementXml = element.asXML();
            ByteArrayInputStream input = new ByteArrayInputStream(elementXml.getBytes("UTF-8"));
            try
            {
                configuration = JAXB.unmarshal(input, jaxbClass);
            }
            catch (RuntimeException exception)
            {
                throw new RuntimeException("Couldn't parse the XML into " + jaxbClass.getCanonicalName() + ": " + elementXml, exception);
            }
        }
        catch (UnsupportedEncodingException e)
        {
            throw new PluginParseException("You've got a weird JVM which doesn't support UTF-8");
        }

        init(plugin, configuration);
    }
    
    /**
     * Implementations may implement this method but don't have to.
     */
    @Override
    public M getModule()
    {
        return null;
    }

    public T getConfiguration()
    {
        return configuration;
    }

    /**
     * Convenience method to list instances of plugin points in the atlassian-plugin.xml files and return their configuration.
     * It is also possible to retrieve them using {@link PluginAccessor#getEnabledModuleDescriptorsByClass(Class)} then to call getConfiguration()
     * on the returned objects.
     * 
     * @param pluginPointClass
     *            the class that you want plugin points of
     * @param pluginAccessor
     *            the plugin accessor which you can get injected from your contructor
     * @return instances of the plugin point
     */
    public static <T extends JaxbAbstractModuleDescriptor.Bean, U extends JaxbAbstractModuleDescriptor<T, ?>> List<T> getInstances(
            Class<? extends U> pluginPointClass, PluginAccessor pluginAccessor)
    {
        List<? extends U> moduleDescriptors = pluginAccessor.getEnabledModuleDescriptorsByClass(pluginPointClass);
        List<T> beans = Lists.transform(moduleDescriptors, new Function<JaxbAbstractModuleDescriptor<T, ?>, T>()
        {
            @Override
            public T apply(JaxbAbstractModuleDescriptor<T, ?> descriptor)
            {
                return (T) descriptor.getConfiguration();
            }
        });

        return ImmutableList.copyOf(beans);
    }

    /**
     * Marker interface that tells this bean is a configuration bean for the plugin framework.
     * <p>
     * This interface is especially used by developers and helps to discover plugin points.
     * </p>
     */
    public interface Bean
    {
        // No required method, this is a marker interface
    }
}
