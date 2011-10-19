package com.atlassian.plugin.descriptors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.atlassian.plugin.*;
import com.atlassian.plugin.elements.DescriptionDescriptor;
import com.atlassian.plugin.elements.JavaVersionDescriptor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.util.JavaVersionUtils;
import com.atlassian.util.concurrent.NotNull;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.plugin.util.Assertions.notNull;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AbstractMarshalledDescriptor<T> implements ModuleDescriptor<T>, StateAware
{
    @XmlAttribute(required = true)
    protected String key;

    @XmlAttribute
    protected String name;

    @XmlAttribute(name = "i18n-name-key")
    private String i18nNameKey;

    @XmlAttribute(name = "class")
    protected String moduleClassName;

    @XmlElement(name = "description")
    protected DescriptionDescriptor descriptionDescriptor;


    @XmlAttribute(name = "state")
    protected String enabledState;

    @XmlAttribute(name = "system")
    protected Boolean systemModule;

    @XmlElement(name = "java-version")
    protected JavaVersionDescriptor javaVersion;

    @XmlTransient
    Map<String, String> params = new HashMap<String, String>();

    @XmlTransient
    protected Resources resources = Resources.EMPTY_RESOURCES;

    @XmlTransient
    boolean enabled = false;

    @XmlTransient
    protected final ModuleFactory moduleFactory;

    @XmlTransient
    private final Logger log = LoggerFactory.getLogger(getClass());

    @XmlTransient
    private final AbstractModuleDescriptor<T> delegatedModuleDescriptor;

    protected AbstractMarshalledDescriptor()
    {
        this.moduleFactory = ModuleFactory.LEGACY_MODULE_FACTORY;

        delegatedModuleDescriptor = new AbstractModuleDescriptor<T>(moduleFactory)
        {
            @Override
            public T getModule()
            {
                return null;
            }
        };
    }

    @Override
    public void init(@NotNull Plugin plugin, @NotNull Element element) throws PluginParseException
    {
        delegatedModuleDescriptor.init(plugin, element);
    }

    /**
     * Method called when the module is initialized
     * initialisation.
     *
     * @param plugin
     *            the plugin which generated this instance
     */
    public abstract void init(Plugin plugin);

    @Override
    public String getCompleteKey()
    {
        return delegatedModuleDescriptor.getCompleteKey();
    }

    @Override
    public String getPluginKey()
    {
        return delegatedModuleDescriptor.getPluginKey();
    }

    @Override
    public String getKey()
    {
        return key;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getDescription()
    {
        return descriptionDescriptor.getDescription();
    }

    @Override
    public Class<T> getModuleClass()
    {
        return delegatedModuleDescriptor.getModuleClass();
    }

    @Override
    public T getModule()
    {
        return delegatedModuleDescriptor.getModule();
    }

    @Override
    public boolean isEnabledByDefault()
    {
       return enabledState == null ? true : enabledState.equalsIgnoreCase("disabled") ? false : true;
    }

    @Override
    public boolean isSystemModule()
    {
        return systemModule == null ? false : systemModule;
    }

    @Override
    public void destroy(Plugin plugin)
    {
        //override if needed
    }

    @Override
    public Float getMinJavaVersion()
    {
        return javaVersion.getMin();
    }

    @Override
    public boolean satisfiesMinJavaVersion()
    {
        if (getMinJavaVersion() != null)
        {
            return JavaVersionUtils.satisfiesMinVersion(getMinJavaVersion());
        }
        return true;
    }

    @Override
    public Map<String, String> getParams()
    {
        return params;
    }

    @Override
    public String getI18nNameKey()
    {
        return i18nNameKey;
    }

    @Override
    public String getDescriptionKey()
    {
        return descriptionDescriptor.getKey();
    }

    @Override
    public Plugin getPlugin()
    {
        return delegatedModuleDescriptor.getPlugin();
    }

    @Override
    public List<ResourceDescriptor> getResourceDescriptors()
    {
        return resources.getResourceDescriptors();
    }

    @Override
    public List<ResourceDescriptor> getResourceDescriptors(String type)
    {
        return resources.getResourceDescriptors(type);
    }

    @Override
    public ResourceDescriptor getResourceDescriptor(String type, String name)
    {
        return resources.getResourceDescriptor(type, name);
    }

    @Override
    public ResourceLocation getResourceLocation(String type, String name)
    {
        return resources.getResourceLocation(type, name);
    }

    @Override
    public void enabled()
    {
        delegatedModuleDescriptor.enabled();
        this.enabled = true;
    }

    @Override
    public void disabled()
    {
        this.enabled = false;
        delegatedModuleDescriptor.disabled();
    }

    @Override
    public boolean equals(Object obj)
    {
        return new ModuleDescriptors.EqualsBuilder().descriptor(this).isEqualTo(obj);
    }

    @Override
    public int hashCode()
    {
        return new ModuleDescriptors.HashCodeBuilder().descriptor(this).toHashCode();
    }

    @Override
    public String toString()
    {
        return getCompleteKey() + " (" + getDescription() + ")";
    }

}
