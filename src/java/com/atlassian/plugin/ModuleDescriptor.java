package com.atlassian.plugin;

import org.dom4j.Element;

public interface ModuleDescriptor
{
    String getKey();

    String getName();

    Class getModuleClass();

    Object getModule();

    String getDescription();

    void init(Element element) throws PluginParseException;
}
