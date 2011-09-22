package com.atlassian.plugin.metadata;

import com.atlassian.plugin.PluginAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class RequiredPluginValidator
{
    private static final Logger log = LoggerFactory.getLogger(RequiredPluginValidator.class);

    private final PluginAccessor pluginAccessor;
    private final RequiredPluginProvider requiredPluginProvider;
    private final Collection<String> errors;

    public RequiredPluginValidator(final PluginAccessor pluginAccessor, final RequiredPluginProvider requiredPluginProvider)
    {
        this.pluginAccessor = pluginAccessor;
        this.requiredPluginProvider = requiredPluginProvider;
        errors = new HashSet<String>();
    }

    /**
     * Validates that the plugins specified in the {@link ClasspathFilePluginMetadata} are enabled.
     *
     * Returns a Collection of all of the keys that did not validate
     *
     * @return A Collection of plugin and module keys that did not validate.
     */
    public Collection<String> validate()
    {
        for (String key : requiredPluginProvider.getRequiredPluginKeys())
        {
            if (!pluginAccessor.isPluginEnabled(key))
            {
                log.error("Plugin Not Enabled: " + key);
                errors.add(key);
            }
        }

        for (String key : requiredPluginProvider.getRequiredModuleKeys())
        {
            if (!pluginAccessor.isPluginModuleEnabled(key))
            {
                log.error("Plugin Module Not Enabled: " + key);
                errors.add(key);
            }
        }

        return errors;
    }
}
