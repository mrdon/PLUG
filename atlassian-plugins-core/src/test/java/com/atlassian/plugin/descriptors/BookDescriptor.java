package com.atlassian.plugin.descriptors;

import javax.xml.bind.annotation.*;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;

/**
 * @since version
 */
@XmlRootElement(name = "book2")
@XmlAccessorType(XmlAccessType.FIELD)
public class BookDescriptor extends AbstractMarshalledDescriptor<Void>
{
    @XmlTransient
    private PluginAccessor pluginAccessor;

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

    public PluginAccessor getPluginAccessor()
    {
        return pluginAccessor;
    }

    public void setPluginAccessor(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }

    @Override
    public String toString()
    {
        return "Book [title=" + getTitle() + ", key=" + getKey() + "]";
    }
}
