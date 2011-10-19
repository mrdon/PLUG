package com.atlassian.plugin.descriptors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.plugin.Plugin;

/**
 * @since version
 */
@XmlRootElement(name = "book2")
@XmlAccessorType(XmlAccessType.FIELD)
public class BookDescriptor extends AbstractMarshalledDescriptor<Void>
{
    @XmlElement
    protected String title;

    @Override
    public void init(Plugin plugin)
    {
        //don't need anything
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @Override
    public String toString()
    {
        return "Book [title=" + getTitle() + ", key=" + getKey() + "]";
    }
}
