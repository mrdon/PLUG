package com.atlassian.plugin.elements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since version
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "java-version")
public class JavaVersionDescriptor
{
    @XmlAttribute
    protected float min;

    @XmlAttribute
    protected float max;

    public float getMin()
    {
        return min;
    }

    public float getMax()
    {
        return max;
    }
}
