package com.atlassian.plugin.osgi;

import com.atlassian.plugin.web.descriptors.DefaultWebItemModuleDescriptor;
import com.atlassian.plugin.web.DefaultWebInterfaceManager;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class DummyWebItemModuleDescriptor extends DefaultWebItemModuleDescriptor
{
    private String key;

    public DummyWebItemModuleDescriptor() {
        super(new DefaultWebInterfaceManager());
        Element e = DocumentHelper.createElement("somecrap");
        e.addAttribute("key", "foo");
        init(null, e);
        this.key = "somekey";
    }
    public String getCompleteKey()
    {
        return "test.plugin:somekey";
    }

    public String getPluginKey()
    {
        return "test.plugin";
    }

    public String getKey()
    {
        return key;
    }
}
