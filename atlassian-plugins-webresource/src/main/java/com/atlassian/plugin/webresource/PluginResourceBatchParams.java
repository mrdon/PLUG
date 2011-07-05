package com.atlassian.plugin.webresource;

import com.atlassian.util.concurrent.NotNull;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a particular type of resource.
 * i.e. css or,
 * js and ieonly
 */
class PluginResourceBatchParams
{
    private final String type;
    private final Map<String, String> parameters;

    public PluginResourceBatchParams(@NotNull String type, @NotNull Map<String, String> parameters)
    {
        this.type = type;
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    public String getType()
    {
        return type;
    }

    public Map<String, String> getParameters()
    {
        return parameters;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PluginResourceBatchParams that = (PluginResourceBatchParams) o;

        if (!parameters.equals(that.parameters)) return false;
        if (!type.equals(that.type)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = type.hashCode();
        result = 91 * result + parameters.hashCode();
        return result;
    }
}