package com.atlassian.plugin.osgi.factory.descriptor;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;

/**
 * Module descriptor for Spring components.  Shouldn't be directly used outside providing read-only information.
 *
 * @since 2.2.0
 */
public class ComponentModuleDescriptor extends AbstractModuleDescriptor<Void>
{

    public Void getModule()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void loadClass(Plugin plugin, String clazz) throws PluginParseException
    {
        // do nothing
    }

    /**
     * @deprecated - BEWARE that this is a temporary method that will not exist for long. Deprecated since 2.3.0
     *
     * @return Module Class Name
     * @since 2.3.0
     */
    public String getModuleClassName()
    {
        return moduleClassName;
    }
}