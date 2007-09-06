package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.*;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.loaders.LoaderUtils;
import com.atlassian.plugin.modulefactory.ModuleFactory;
import com.atlassian.plugin.util.JavaVersionUtils;
import org.dom4j.Element;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

/**
 * TODO: add the check if getModuleBaseType() != null that it is assignnable from the object returned from ModuleFactory.getModule()
 *
 */
public abstract class AbstractModuleDescriptor implements ModuleDescriptor
{
    protected Plugin plugin;
    String key;
    String name;
    Class moduleClass;
    String description;
    boolean enabledByDefault = true;
    boolean systemModule = false;
    protected boolean singleton = true;
    Map params;
    protected Resources resources;
    private Float minJavaVersion;
    private String i18nNameKey;
    private String descriptionKey;
    private ModuleFactory moduleFactory;

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        this.plugin = plugin;
        this.key = element.attributeValue("key");
        this.name = element.attributeValue("name");
        this.i18nNameKey = element.attributeValue("i18n-name-key");

        String clazz = element.attributeValue("class");
        try
        {
            if (clazz != null)  //not all plugins have to have a class
            {
                // First try and load the class, to make sure the class exists
                moduleClass = plugin.loadClass(clazz, getClass());

                // Then instantiate the class, so we can see if there are any dependencies that aren't satisfied
                try
                {
                    Constructor noargConstructor = moduleClass.getConstructor(new Class[]{});
                    if(noargConstructor != null)
                    {
                        moduleClass.newInstance();
                    }
                }
                catch (NoSuchMethodException e)
                {
                    // If there is no "noarg" constructor then don't do the check
                }
            }
        }
        catch (ClassNotFoundException e)
        {
            throw new PluginParseException("Could not load class: " + clazz, e);
        }
        catch (NoClassDefFoundError e)
        {
            throw new PluginParseException("Error retrieving dependency of class: " + clazz + ". Missing class: " + e.getMessage());
        }
        catch (UnsupportedClassVersionError e)
        {
            throw new PluginParseException("Class version is incompatible with current JVM: " + clazz, e);
        }
        catch (Throwable t)
        {
            throw new PluginParseException(t);
        }

        this.description = element.elementTextTrim("description");
        Element descriptionElement = element.element("description");
        this.descriptionKey = (descriptionElement != null) ? descriptionElement.attributeValue("key") : null;
        params = LoaderUtils.getParams(element);

        if ("disabled".equalsIgnoreCase(element.attributeValue("state")))
        {
            enabledByDefault = false;
        }

        if ("true".equalsIgnoreCase(element.attributeValue("system")))
        {
            systemModule = true;
        }

        if (element.element("java-version") != null)
        {
            minJavaVersion = Float.valueOf(element.element("java-version").attributeValue("min"));
        }

        if ("false".equalsIgnoreCase(element.attributeValue("singleton")))
        {
            singleton = false;
        }
        else if ("true".equalsIgnoreCase(element.attributeValue("singleton")))
        {
            singleton = true;
        }
        else
        {
            singleton = isSingletonByDefault();
        }

        resources = Resources.fromXml(element);

        // ModuleDescriptors
        final String moduleFactoryName = element.attributeValue("module-factory");
        if (moduleFactoryName != null)
        {
            moduleFactory = createModuleFactory(moduleFactoryName);
        }
    }

    /**
     * Creates a ModuleFactory instance of the given type name using the default constructor or
     * if this can't be done, returns null.
     * @param moduleFactoryName the name of the ModuleFactory class.
     * @return an instance of the ModuleFactory, may be null.
     */
    private ModuleFactory createModuleFactory(final String moduleFactoryName) {
        ModuleFactory mf = null;
        try {
            Class factoryClass = plugin.loadClass(moduleFactoryName, getClass());
            mf = (ModuleFactory) factoryClass.newInstance();
            mf.setModuleDescriptor(this);
            // TODO log these exceptions properly
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return mf;
    }

    /**
     * Override this if your plugin needs to clean up when it's been removed.
     * @param plugin
     */
    public void destroy(Plugin plugin)
    {}

    public boolean isEnabledByDefault()
    {
        return enabledByDefault && satisfiesMinJavaVersion();
    }

    public boolean isSystemModule()
    {
        return systemModule;
    }

    public boolean isSingleton()
    {
        return singleton;
    }

    /**
     * Override this method if you want your module descriptor to be or not be a singleton by default.
     * <p>
     * Default is "true" - ie all plugin modules are singletons by default.
     */
    protected boolean isSingletonByDefault()
    {
        return true;
    }

    /**
     * Check that the module class of this descriptor implements a given interface, or extends a given class.
     * @param requiredModuleClazz The class this module's class must implement or extend.
     * @throws PluginParseException If the module class does not implement or extend the given class.
     */
    final protected void assertModuleClassImplements(Class requiredModuleClazz) throws PluginParseException
    {
        if (!requiredModuleClazz.isAssignableFrom(getModuleClass()))
        {
            throw new PluginParseException("Given module class: " + getModuleClass().getName() + " does not implement " + requiredModuleClazz.getName());
        }
    }

    public String getCompleteKey() {
        return plugin.getKey() + ":" + getKey();
    }

    public String getPluginKey()
    {
        return plugin.getKey();
    }

    public String getKey()
    {
        return key;
    }

    public String getName()
    {
        return name;
    }

    public Class getModuleClass()
    {
        // TODO need a better way of having two ways to check the module type - remember some plugins don't have a module class but plugins should be reachable by PluginManager.getEnabledModulesByClass()
        if (moduleClass == null) {
            return getModuleBaseType();
        }
        return moduleClass;
    }


    /**
     * By default ModuleDescriptors do not require a base type, so this implementation returns null.
     * Override this in your ModuleDescriptor implementation to declare the high level interface that should be
     * implemented by module objects returned by {@link @getModule()}
     * @return
     */
    public Class getModuleBaseType()
    {
        return null;
    }

    /**
     * Default implementation is to use the configured ModuleFactory. <strong>Do not override this, instead, override
     * {@link #getModuleFactory()} and put your module creation code in there.</strong>. In the bad old days, overriding
     * this was what all the cool kids did. In future this method will be made final so ModuleDescriptors will be
     * forced to customise module instantiation code in a ModuleFactory. This makes more sense anyway since there will
     * usually be a much smaller number of module instantiation strategies than ModuleDescriptors and these can
     * therefore be reused as instances of the ModuleFactory abstraction.
     *
     * @return the instance of the module.
     * @throws IllegalStateException if there is no configured ModuleFactory.
     */
    public Object getModule() throws IllegalStateException {
        return getModuleFactory().getModule();
    }

    /**
     * Get the ModuleFactory responsible for providing module instances. The default implementation is to provide the
     * ModuleFactory defined in the plugin module's configuration. Override this if you want to control module instance
     * creation.
     *
     * @return the ModuleFactory.
     */
    public ModuleFactory getModuleFactory() {
        return moduleFactory;
    }

    public String getDescription()
    {
        return description;
    }

    public Map getParams()
    {
        return params;
    }

    public String getI18nNameKey()
    {
        return i18nNameKey;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDescriptionKey()
    {
        return descriptionKey;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List getResourceDescriptors()
    {
        return resources.getResourceDescriptors();
    }

    public List getResourceDescriptors(String type)
    {
        return resources.getResourceDescriptors(type);
    }

    public ResourceLocation getResourceLocation(String type, String name)
    {
        return resources.getResourceLocation(type, name);
    }

    /**
     * @deprecated
     */
    public ResourceDescriptor getResourceDescriptor(String type, String name)
    {
        return resources.getResourceDescriptor(type, name);
    }

    public Float getMinJavaVersion()
    {
        return minJavaVersion;
    }

    public boolean satisfiesMinJavaVersion()
    {
        if(minJavaVersion != null)
        {
            return JavaVersionUtils.satisfiesMinVersion(minJavaVersion.floatValue());
        }
        return true;
    }

    /**
     * Sets the plugin for the ModuleDescriptor
     *
     * @param plugin
     */
    public void setPlugin(Plugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * @return The plugin this module descriptor is associated with
     */
    public Plugin getPlugin()
    {
        return plugin;
    }

    public String toString()
    {
        return getCompleteKey() + " (" + getDescription() + ")";
    }
}
