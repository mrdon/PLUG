package com.atlassian.plugin.module;

import com.atlassian.plugin.PluginParseException;

/**
 * If a module class could not be found
 *
 * @since 2.5
 */
public class ModuleClassNotFoundException extends PluginParseException
{
    private final String className;
    private final String pluginKey;
    private final String moduleKey;

    public ModuleClassNotFoundException(String className, String pluginKey, String moduleKey, ClassNotFoundException ex)
    {
        super(ex);
        this.className = className;
        this.pluginKey = pluginKey;
        this.moduleKey = moduleKey;
    }

    public String getClassName()
    {
        return className;
    }

    public String getPluginKey()
    {
        return pluginKey;
    }

    public String getModuleKey()
    {
        return moduleKey;
    }

    @Override
    public String getMessage()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Couldn't load the class '").append(className).append("'. ");
        builder.append("This could mean that you misspelled the name of the class (doublecheck) or that ");
        builder.append("you're using a class in your plugin that you haven't provided bundle instructions for.");
        builder.append("See http://confluence.atlassian.com/x/QRS-Cg for more details on how to fix this.");
        return builder.toString();
    }
}
