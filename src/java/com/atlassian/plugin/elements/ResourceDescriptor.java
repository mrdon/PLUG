package com.atlassian.plugin.elements;

import com.atlassian.plugin.loaders.LoaderUtils;
import org.dom4j.Element;

import java.util.Map;
import java.util.regex.Pattern;

public class ResourceDescriptor
{
    private String type;
    private String name;
    private String location;
    private String contentType;

    private Pattern pattern;
    private String content;
    private Map params;
    private ResourceLocation ourLocation;

    public ResourceDescriptor(Element element)
    {
        this.type = element.attributeValue("type");
        String name = element.attributeValue("name");
        String namePattern = element.attributeValue("namePattern");
        if (name == null && namePattern == null)
        {
            throw new RuntimeException("resource descriptor needs one of 'name' and 'namePattern' attributes.");
        }

        if (name != null && namePattern != null)
        {
            throw new RuntimeException("resource descriptor can have only one of 'name' and 'namePattern' attributes.");
        }

        this.name = name;

        this.location = element.attributeValue("location");

        if (namePattern != null && !location.endsWith("/"))
        {
            throw new RuntimeException("when 'namePattern' is specified, 'location' must be a directory (ending in '/')");
        }
        this.params = LoaderUtils.getParams(element);

        if (element.getTextTrim() != null && !"".equals(element.getTextTrim()))
        {
            content = element.getTextTrim();
        }

        contentType = getParameter("content-type");

        if (namePattern != null)
        {
            pattern = Pattern.compile(namePattern);
        }
        else
        {
            ourLocation = new ResourceLocation(location, name, type, contentType, content, params);
        }

    }

    public String getType()
    {
        return type;
    }

    /**
     * This may throw an exception if one of the deprecated methods is used on a ResourceDescriptor which has been given a namePattern
     */
    public String getName()
    {
        if (name == null)
        {
            throw new RuntimeException("tried to get name from ResourceDescriptor with null name and namePattern = " + pattern);
        }
        return name;
    }

    public String getLocation()
    {
        return location;
    }


    public String getContent()
    {
        return content;
    }

    public boolean doesTypeAndNameMatch(String type, String name)
    {
        if (type != null && type.equalsIgnoreCase(this.type))
        {
            if (pattern != null)
            {
                return pattern.matcher(name).matches();
            }
            else
            {
                return name != null && name.equalsIgnoreCase(this.name);
            }
        }
        else
        {
            return false;
        }
    }


    public String getParameter(String key)
    {
        return (String) params.get(key);
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ResourceDescriptor))
        {
            return false;
        }

        final ResourceDescriptor resourceDescriptor = (ResourceDescriptor) o;

        if (!name.equals(resourceDescriptor.name))
        {
            return false;
        }
        if (!type.equals(resourceDescriptor.type))
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result;
        result = type.hashCode();
        result = 29 * result + name.hashCode();
        return result;
    }

    public ResourceLocation getResourceLocationForName(String name)
    {

        if (pattern != null)
        {
            if (pattern.matcher(name).matches())
            {
                return new ResourceLocation(getLocation(), name, type, contentType, content, params);
            }
            else
            {
                throw new RuntimeException("Thiss descriptor does not provide resources named " + name);
            }
        }
        else
        {
            return ourLocation;
        }
    }
}
