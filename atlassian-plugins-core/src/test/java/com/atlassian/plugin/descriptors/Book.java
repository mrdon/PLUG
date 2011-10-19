package com.atlassian.plugin.descriptors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.plugin.elements.AbstractJaxbConfigurationBean;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Book extends AbstractJaxbConfigurationBean
{
    @XmlElement
    protected String title;

    
    public Book()
    {
        super();
    }

    public Book(String title)
    {
        super();
        this.title = title;
    }

    @Override
    public String toString()
    {
        return "Book [title=" + getTitle() + ", key=" + getKey() + "]";
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }
    
}