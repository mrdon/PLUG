package com.atlassian.plugin.elements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.atlassian.plugin.descriptors.JaxbAbstractModuleDescriptor;

/**
 * JAXB bean which declares the common properties of modules.
 * @see JaxbAbstractModuleDescriptor
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AbstractJaxbConfigurationBean
{
    @XmlAttribute
    protected String key;    

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }
}
