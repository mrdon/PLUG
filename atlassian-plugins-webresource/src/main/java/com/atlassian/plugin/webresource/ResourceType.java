package com.atlassian.plugin.webresource;

import java.util.Map;

/**
 * Represents a particular type of resource.
 * i.e. css or,
 * js and ieonly
 */
public class ResourceType
{
    private String type;
    private Map<String, String> parameters;

    public ResourceType(String type, Map<String, String> parameters)
    {
        this.type = type;
        this.parameters = parameters;
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

        ResourceType that = (ResourceType) o;

        if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = type != null ? type.hashCode() : 0;
        result = 91 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }
}