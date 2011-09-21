package com.atlassian.plugin.metadata;

import com.atlassian.plugin.PluginAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

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
        errors = new ArrayList<String>();
    }

    /**
     * Validates that the plugins specified in the {@link ClasspathFilePluginMetadata} are enabled.
     *
     * Returns true if all of the plugins and modules are enabled, false otherwise.
     *
     * @return True if the plugins and modules are enabled, false otherwise.
     */
    public boolean validate()
    {
        boolean isValid = true;

        for (String key : requiredPluginProvider.getRequiredPluginKeys())
        {
            if (!pluginAccessor.isPluginEnabled(key))
            {
                isValid = false;
                log.error("Plugin Not Enabled: " + key);
                errors.add(key);
            }
        }

        for (String key : requiredPluginProvider.getRequiredModuleKeys())
        {
            if (!pluginAccessor.isPluginModuleEnabled(key))
            {
                isValid = false;
                log.error("Plugin Module Not Enabled: " + key);
                errors.add(key);
            }
        }

        return isValid;
    }

    /**
     * The collection of error strings if the validation failed. An empty collection otherwise.
     *
     * @return A collection of error strings.
     */
    public Collection<String> getErrors()
    {
        return errors;
    }
}
