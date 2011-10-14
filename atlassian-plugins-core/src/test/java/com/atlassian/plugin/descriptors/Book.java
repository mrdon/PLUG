package com.atlassian.plugin.descriptors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Book implements JaxbAbstractModuleDescriptor.Bean
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
        return "Book [title=" + getTitle() + "]";
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