package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import org.dom4j.Element;

/**
 * Instances of this class represent a module which <i>could not be loaded</i>, not a module
 * which <i>can be unloaded</i>.
 */
public class UnloadableModuleDescriptor extends AbstractModuleDescriptor
{
    private String errorText;

    public Object getModule()
    {
        return null;
    }

    protected void loadClass(Plugin plugin, Element element) throws PluginParseException {
        // don't try to load the class -- we are possibly here because it doesn't exist
    }

    public boolean isEnabledByDefault()
    {
        // An Unloadable module is never enabled
        return false;
    }

    public String getErrorText()
    {
        return errorText;
    }

    public void setErrorText(String errorText)
    {
        this.errorText = errorText;
    }

    /**
     * Sets the key of the ModuleDescriptor
     *
     * This is theoretically bad, as the superclass and the interface doesn't define this method,
     * but it's required to construct an UnloadableModuleDescriptor when we don't have the XML Element.
     *
     * @param key the key of the ModuleDescriptor
     */
    public void setKey(String key)
    {
        this.key = key;
    }

    /**
     * Sets the name of the ModuleDescriptor
     *
     * This is theoretically bad, as the superclass and the interface doesn't define this method,
     * but it's required to construct an UnloadableModuleDescriptor when we don't have the XML Element.
     *
     * @param name the name of the ModuleDescriptor
     */
    public void setName(String name)
    {
        this.name = name;
    }
}
