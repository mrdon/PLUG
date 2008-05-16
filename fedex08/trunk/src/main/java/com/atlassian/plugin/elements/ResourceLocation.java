package com.atlassian.plugin.elements;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.HashMap;
import java.util.Map;

/**
 * This class gives the location of a particular resource
 */
public class ResourceLocation
{
    private String location;
    private String name;
    private String type;
    private String contentType;
    private String content;
    private Map params;

    public ResourceLocation(String location, String name, String type, String contentType, String content, Map params)
    {
        this.location = location;
        this.name = name;
        this.type = type;
        this.contentType = contentType;
        this.content = content;
        this.params = params;
    }

    /**
     * A copy constructor that allows you to create a new ResourceLocation based on a previous one,
     * possibly changing the location.
     *
     * @param location         the new location
     * @param copyThisLocation the ResourceLocation to copy otherwise
     */
    public ResourceLocation(String location, ResourceLocation copyThisLocation)
    {
        this.location = location;
        this.name = copyThisLocation.name;
        this.type = copyThisLocation.type;
        this.contentType = copyThisLocation.contentType;
        this.content = copyThisLocation.content;
        this.params = new HashMap(copyThisLocation.params);
    }

    public String getLocation()
    {
        return location;
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getContent()
    {
        return content;
    }

    public String getParameter(String key)
    {
        return (String) params.get(key);
    }

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
