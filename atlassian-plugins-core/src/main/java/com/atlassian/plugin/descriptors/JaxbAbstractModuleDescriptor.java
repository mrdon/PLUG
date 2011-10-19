package com.atlassian.plugin.descriptors;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXB;

import org.dom4j.Element;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.elements.AbstractJaxbConfigurationBean;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
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
 * @param <C>
 *            the JAXB class that you want to read your configuration into
 * @param <M>
 *            the module class. You may use Void.
 * @since 2.11
 * 
 * 
 */
public abstract class JaxbAbstractModuleDescriptor<C extends AbstractJaxbConfigurationBean, M> implements ModuleDescriptor<M>, StateAware
{
    /** The Jaxb bean into which the configuration is parsed */
    protected C configuration;
    private final Class<C> jaxbClass;

    private final AbstractModuleDescriptor<M> delegatedModuleDescriptor;
    protected ModuleFactory moduleFactory;

    public JaxbAbstractModuleDescriptor(ModuleFactory moduleFactory, Class<C> jaxbClass)
    {
        delegatedModuleDescriptor = new AbstractModuleDescriptor<M>(moduleFactory)
        {
            @Override
            public M getModule()
            {
                return null;
            }
        };
        this.moduleFactory = moduleFactory;
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
    public abstract void init(Plugin plugin, C configuration);

    /**
     * Implements the module by transforming 'element' into 'configuration' using JAXB
     */
    @Override
    final public void init(Plugin plugin, Element element) throws PluginParseException
    {
        delegatedModuleDescriptor.init(plugin, element);

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
     * Returns the configuration of this module descriptor.
     * @return the configuration
     */
    protected C getConfiguration()
    {
        return configuration;
    }

    public void destroy(Plugin plugin)
    {
        delegatedModuleDescriptor.destroy(plugin);
    }

    public boolean isEnabledByDefault()
    {
        return delegatedModuleDescriptor.isEnabledByDefault();
    }

    public boolean isSystemModule()
    {
        return delegatedModuleDescriptor.isSystemModule();
    }

    public String getCompleteKey()
    {
        return delegatedModuleDescriptor.getCompleteKey();
    }

    public String getPluginKey()
    {
        return delegatedModuleDescriptor.getPluginKey();
    }

    public String getKey()
    {
        return delegatedModuleDescriptor.getKey();
    }

    public String getName()
    {
        return delegatedModuleDescriptor.getName();
    }

    public Class<M> getModuleClass()
    {
        return delegatedModuleDescriptor.getModuleClass();
    }

    public String getDescription()
    {
        return delegatedModuleDescriptor.getDescription();
    }

    public Map<String, String> getParams()
    {
        return delegatedModuleDescriptor.getParams();
    }

    public String getI18nNameKey()
    {
        return delegatedModuleDescriptor.getI18nNameKey();
    }

    public String getDescriptionKey()
    {
        return delegatedModuleDescriptor.getDescriptionKey();
    }

    public List<ResourceDescriptor> getResourceDescriptors()
    {
        return delegatedModuleDescriptor.getResourceDescriptors();
    }

    public List<ResourceDescriptor> getResourceDescriptors(String type)
    {
        return delegatedModuleDescriptor.getResourceDescriptors(type);
    }

    public ResourceLocation getResourceLocation(String type, String name)
    {
        return delegatedModuleDescriptor.getResourceLocation(type, name);
    }

    public ResourceDescriptor getResourceDescriptor(String type, String name)
    {
        return delegatedModuleDescriptor.getResourceDescriptor(type, name);
    }

    public Float getMinJavaVersion()
    {
        return delegatedModuleDescriptor.getMinJavaVersion();
    }

    public boolean satisfiesMinJavaVersion()
    {
        return delegatedModuleDescriptor.satisfiesMinJavaVersion();
    }

    /**
     * Sets the plugin for the ModuleDescriptor
     * 
     * @param plugin
     *            The plugin to set for this descriptor.
     */
    public void setPlugin(Plugin plugin)
    {
        delegatedModuleDescriptor.setPlugin(plugin);
    }

    public Plugin getPlugin()
    {
        return delegatedModuleDescriptor.getPlugin();
    }

    public boolean equals(Object obj)
    {
        return new ModuleDescriptors.EqualsBuilder().descriptor(this).isEqualTo(obj);
    }

    public int hashCode()
    {
        return new ModuleDescriptors.HashCodeBuilder().descriptor(this).toHashCode();
    }

    public String toString()
    {
        return delegatedModuleDescriptor.toString();
    }

    /**
     * Enables the descriptor by loading the module class. Classes overriding
     * this method MUST call super.enabled() before their own enabling code.
     * 
     * @since 2.1.0
     */
    public void enabled()
    {
        delegatedModuleDescriptor.enabled();
    }

    /**
     * Disables the module descriptor. Classes overriding this method MUST call
     * super.disabled() after their own disabling code.
     */
    public void disabled()
    {
        delegatedModuleDescriptor.disabled();
    }

}
