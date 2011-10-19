package com.atlassian.plugin.elements;

import javax.xml.bind.annotation.*;

/**
 * @since version
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "description")
public class DescriptionDescriptor
{
    @XmlAttribute
    protected String key;
    
    @XmlValue
    protected String description;

    public String getKey()
    {
        return key;
    }

    public String getDescription()
    {
        return description;
    }
}
