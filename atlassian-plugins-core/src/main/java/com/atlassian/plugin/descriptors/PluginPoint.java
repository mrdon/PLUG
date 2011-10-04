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
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.util.validation.ValidationPattern;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Convenience class to create a plugin point:
 * <ul>
 * As a plugin point provider, this is your check-list:
 * <li>Extend this class and implement {@link #chooseJaxbClass(Element)} to tell in which class the atlassian-plugin.xml configuration must be poured.</li>
 * <li>Declare it in atlassian-plugin.xml: {@literal <module-type key="my-plugin-point" class="org.example.TestPluginPoint"  />}</li>
 * <li>If you want to use this plugin point outside of your project, make sure you tell AMPS to export the class (in the pom.xml):
 * 
 * <pre>
 * {@literal
 *     <plugin>
 *         <groupId>com.atlassian.maven.plugins</groupId>
 *         <artifactId>maven-amps-plugin</artifactId>
 *         <version>...</version>
 *         <extensions>true</extensions>
 *         <configuration>
 *             <product>...</product>
 *             <instructions>
 *                 <Export-Package>org.example</Export-Package>
 *             </instructions>
 *             ...
 * }
 * </pre>
 * 
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
 * <p>
 * Please note that no contract is given on "extends AbstractModuleDescriptor", which is an implementation detail.
 * We may extend any other class later, as long as it implements {@literal ModuleDescriptor<T>}.
 * </p>
 * 
 * @param <T>
 *            the JAXB class that you want to read your configuration from
 * @since 2.11
 * 
 *  
 */
public abstract class PluginPoint<T extends PluginPoint.Bean> extends AbstractModuleDescriptor<T> implements ModuleDescriptor<T>, StateAware
{
    /** The module is an instance of the JAXB-annotated class. */
    private T module;

    public PluginPoint(ModuleFactory moduleFactory)
    {
        super(moduleFactory);
    }

    /**
     * Implements the module:
     * <ul>
     * <li>by transforming 'element' into 'module' using JAXB,
     * <li>or by using the class attribute to implement the module.
     * </ul>
     */
    @Override
    public final void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        if (this.moduleClassName != null)
        {
            // Don't use JAXB, don't use the contents. Just use the class provided:
            // <my-plugin-point key="myKey" class="MyClass"/>
            // Basically, module = new MyClass(auto-wired-parameters);
            if (this.moduleClass == null)
            {
                loadClass(plugin, this.moduleClassName);
            }
            module = moduleFactory.createModule(moduleClassName, this);

            // Check the user didn't mix up concepts: if you give a class, don't give contents
            if (!element.elements().isEmpty())
            {
                throw new PluginParseException(String.format("In plugin %s, the %s is defined both with a class and " +
                        "contents. The class replaces the contents, so you must specify only " +
                        "one of them. Details: %s", plugin.getName(), element.getName(), element.asXML()));
            }
        }
        else
        {
            // We use JAXB to parse atlassian-plugin.xml into a module bean.
            String elementXml = element.asXML();
            Class<T> jaxbClass = chooseJaxbClass(element);

            try
            {
                ByteArrayInputStream input = new ByteArrayInputStream(elementXml.getBytes("UTF-8"));
                module = JAXB.unmarshal(input, jaxbClass);
            }
            catch (UnsupportedEncodingException e)
            {
                throw new PluginParseException("You've got a weird JVM which doesn't support UTF-8");
            }
        }
    }

    /**
     * The class that you want your atlassian-plugin.xml element to be transferred in.
     * 
     * @param element
     *            the original dom4j element which will
     * @return the class which must be used to create a bean representing the atlassian-plugin.xml element, most probably {@literal T.class}. Must not be null.
     */
    public abstract <U extends T> Class<U> chooseJaxbClass(Element element);

    /**
     * Returns the module. Never null.
     */
    public final T getModule()
    {
        return module;
    }

    /**
     * Convenience method to list instances of plugin points in the atlassian-plugin.xml files.
     * It is also possible to retrieve them using {@link PluginAccessor#getEnabledModuleDescriptorsByClass(Class)} then to call getModule() on the return
     * objects.
     * 
     * @param pluginPointClass
     *            the class that you want plugin points of
     * @param pluginAccessor
     *            the plugin accessor which you can get injected from your contructor
     * @return instances of the plugin point
     */
    public static <T extends PluginPoint.Bean, U extends PluginPoint<T>> List<T> getInstances(Class<? extends U> pluginPointClass, PluginAccessor pluginAccessor)
    {

        List<? extends U> moduleDescriptors = pluginAccessor.getEnabledModuleDescriptorsByClass(pluginPointClass);
        List<T> beans = Lists.transform(moduleDescriptors, new Function<PluginPoint, T>()
        {
            @Override
            public T apply(PluginPoint descriptor)
            {
                return (T) descriptor.getModule();
            }
        });

        return ImmutableList.copyOf(beans);
    }

    // Make the class simple by hiding all other methods to children

    @Override
    final protected void provideValidationRules(ValidationPattern pattern)
    {
        super.provideValidationRules(pattern);
    }

    @Override
    final protected void loadClass(Plugin plugin, Element element) throws PluginParseException
    {
        super.loadClass(plugin, element);
    }

    @Override
    final protected void loadClass(Plugin plugin, String clazz) throws PluginParseException
    {
        super.loadClass(plugin, clazz);
    }

    @Override
    final Class<?> getModuleReturnClass()
    {
        return super.getModuleReturnClass();
    }

    @Override
    final public void destroy(Plugin plugin)
    {
        super.destroy(plugin);
    }

    @Override
    final public boolean isEnabledByDefault()
    {
        return super.isEnabledByDefault();
    }

    @Override
    final public boolean isSystemModule()
    {
        return super.isSystemModule();
    }

    @Override
    final public boolean isSingleton()
    {
        return super.isSingleton();
    }

    @Override
    final protected boolean isSingletonByDefault()
    {
        return super.isSingletonByDefault();
    }

    @Override
    final public String getCompleteKey()
    {
        return super.getCompleteKey();
    }

    @Override
    final public String getPluginKey()
    {
        return super.getPluginKey();
    }

    @Override
    final public String getKey()
    {
        return super.getKey();
    }

    @Override
    final public String getName()
    {
        return super.getName();
    }

    @Override
    final public Class<T> getModuleClass()
    {
        return super.getModuleClass();
    }

    @Override
    final public String getDescription()
    {
        return super.getDescription();
    }

    @Override
    final public Map<String, String> getParams()
    {
        return super.getParams();
    }

    @Override
    final public String getI18nNameKey()
    {
        return super.getI18nNameKey();
    }

    @Override
    final public String getDescriptionKey()
    {
        return super.getDescriptionKey();
    }

    @Override
    final public List<ResourceDescriptor> getResourceDescriptors()
    {
        return super.getResourceDescriptors();
    }

    @Override
    final public List<ResourceDescriptor> getResourceDescriptors(String type)
    {
        return super.getResourceDescriptors(type);
    }

    @Override
    final public ResourceLocation getResourceLocation(String type, String name)
    {
        return super.getResourceLocation(type, name);
    }

    @Override
    final public ResourceDescriptor getResourceDescriptor(String type, String name)
    {
        return super.getResourceDescriptor(type, name);
    }

    @Override
    final public Float getMinJavaVersion()
    {
        return super.getMinJavaVersion();
    }

    @Override
    final public boolean satisfiesMinJavaVersion()
    {
        return super.satisfiesMinJavaVersion();
    }

    @Override
    final public void setPlugin(Plugin plugin)
    {
        super.setPlugin(plugin);
    }

    @Override
    final public Plugin getPlugin()
    {
        return super.getPlugin();
    }

    @Override
    final public boolean equals(Object obj)
    {
        return super.equals(obj);
    }

    @Override
    final public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    final public void enabled()
    {
        super.enabled();
    }

    @Override
    final public void disabled()
    {
        super.disabled();
    }

    /**
     * Marker interface that tells this bean is designed to describe an Atlassian Plugin Point.
     * <p>
     * This interface is especially used by developers and helps the discover plugin points.
     * </p>
     */
    public interface Bean
    {
        // No required method, this is a marker interface
    };

}
