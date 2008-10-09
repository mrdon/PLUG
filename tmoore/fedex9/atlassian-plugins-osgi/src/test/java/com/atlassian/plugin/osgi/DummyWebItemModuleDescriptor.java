package com.atlassian.plugin.osgi;

import com.atlassian.plugin.web.descriptors.DefaultWebItemModuleDescriptor;
import com.atlassian.plugin.web.DefaultWebInterfaceManager;
import org.dom4j.DocumentHelper;

public class DummyWebItemModuleDescriptor extends DefaultWebItemModuleDescriptor
{
    private String key;

    public DummyWebItemModuleDescriptor() {
        super(new DefaultWebInterfaceManager());
        init(null, DocumentHelper.createElement("somecrap"));
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
