package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.model.WebLabel;
import com.atlassian.plugin.web.model.WebParam;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.elements.ResourceDescriptor;
import org.dom4j.Element;

import java.util.Map;
import java.util.List;

/**
 * Wrapper for {@link AbstractWebFragmentModuleDescriptor}, so that it could be extended by application specific
 * wrappers to provide additional methods.
 */
public class DefaultAbstractWebFragmentModuleDescriptor implements StateAware, WebFragmentModuleDescriptor
{
    protected AbstractWebFragmentModuleDescriptor abstractDescriptor;

    public DefaultAbstractWebFragmentModuleDescriptor(AbstractWebFragmentModuleDescriptor abstractDescriptor)
    {
        this.abstractDescriptor = abstractDescriptor;
    }

    public void enabled()
    {
        abstractDescriptor.enabled();
    }

    public void disabled()
    {
        abstractDescriptor.disabled();
    }

    public int getWeight()
    {
        return abstractDescriptor.getWeight();
    }

    public String getKey()
    {
        return abstractDescriptor.getKey();
    }

    public Object getModule()
    {
        return abstractDescriptor.getModule();
    }

    public String getI18nNameKey()
    {
        return abstractDescriptor.getI18nNameKey();
    }

    public String getDescriptionKey()
    {
        return abstractDescriptor.getDescriptionKey();
    }

    public Plugin getPlugin()
    {
        return abstractDescriptor.getPlugin();
    }

    public WebLabel getWebLabel()
    {
        return abstractDescriptor.getWebLabel();
    }

    public WebLabel getTooltip()
    {
        return abstractDescriptor.getTooltip();
    }

    public void setWebInterfaceManager(WebInterfaceManager webInterfaceManager)
    {
        abstractDescriptor.setWebInterfaceManager(webInterfaceManager);
    }

    public Condition getCondition()
    {
        return abstractDescriptor.getCondition();
    }

    public ContextProvider getContextProvider()
    {
        return abstractDescriptor.getContextProvider();
    }

    public WebParam getWebParams()
    {
        return abstractDescriptor.getWebParams();
    }

    //----------------------------------------------------------------------------------------- ModuleDescriptor methods
    public String getCompleteKey()
    {
        return abstractDescriptor.getCompleteKey();
    }

    public String getPluginKey()
    {
        return abstractDescriptor.getPluginKey();
    }

    public String getName()
    {
        return abstractDescriptor.getName();
    }

    public String getDescription()
    {
        return abstractDescriptor.getDescription();
    }

    public Class getModuleClass()
    {
        return abstractDescriptor.getModuleClass();
    }

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        abstractDescriptor.init(plugin, element);
    }

    public boolean isEnabledByDefault()
    {
        return abstractDescriptor.isEnabledByDefault();
    }

    public boolean isSystemModule()
    {
        return abstractDescriptor.isSystemModule();
    }

    public void destroy(Plugin plugin)
    {
        abstractDescriptor.destroy(plugin);
    }

    public Float getMinJavaVersion()
    {
        return abstractDescriptor.getMinJavaVersion();
    }

    public boolean satisfiesMinJavaVersion()
    {
        return abstractDescriptor.satisfiesMinJavaVersion();
    }

    public Map getParams()
    {
        return abstractDescriptor.getParams();
    }

    //------------------------------------------------------------------------------------------------ Resourced methods
    public List getResourceDescriptors()
    {
        return abstractDescriptor.getResourceDescriptors();
    }

    public List getResourceDescriptors(String type)
    {
        return abstractDescriptor.getResourceDescriptors(type);
    }

    public ResourceLocation getResourceLocation(String type, String name)
    {
        return abstractDescriptor.getResourceLocation(type, name);
    }

    public ResourceDescriptor getResourceDescriptor(String type, String name)
    {
        return abstractDescriptor.getResourceDescriptor(type, name);
    }
}
