package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.web.Condition;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.atlassian.plugin.webresource.Dom4jFluent.element;


/**
 *
 */
public class WebResourceModuleDescriptorBuilder
{
    private final Plugin plugin;
    private final String moduleKey;
    private List<Dom4jFluent.Element> resourceDescriptors = new ArrayList<Dom4jFluent.Element>();
    private List<Dom4jFluent.Element> dependencies = new ArrayList<Dom4jFluent.Element>();
    private List<Dom4jFluent.Element> condition = new ArrayList<Dom4jFluent.Element>();
    private List<Dom4jFluent.Element> contexts = new ArrayList<Dom4jFluent.Element>();

    public WebResourceModuleDescriptorBuilder(Plugin plugin, String moduleKey)
    {
        this.plugin = plugin;
        this.moduleKey = moduleKey;
    }


    public WebResourceModuleDescriptorBuilder setCondition(Class<? extends Condition> condition)
    {
        this.condition.add(element("condition", ImmutableMap.of(
                "class", condition.getName())));
        return this;
    }

    public WebResourceModuleDescriptorBuilder addDescriptor(String downloadableFile)
    {
        return addDescriptor("download", downloadableFile, downloadableFile);
    }

    public WebResourceModuleDescriptorBuilder addDescriptor(String type, String name, String location)
    {
        resourceDescriptors.add(element("resource", ImmutableMap.of(
                "type", type,
                "name", name,
                "location", location)));
        return this;
    }

    public WebResourceModuleDescriptorBuilder addDependency(String moduleKey)
    {
        dependencies.add(element("dependency", moduleKey));
        return this;
    }

    public WebResourceModuleDescriptorBuilder addContext(String context)
    {
        contexts.add(element("context", context));
        return this;
    }

    public WebResourceModuleDescriptor build()
    {
        WebResourceModuleDescriptor descriptor = new WebResourceModuleDescriptor(new DefaultHostContainer());
        descriptor.init(plugin, element("web-resource",
                ImmutableMap.of(
                        "key", moduleKey
                ),
                Arrays.<Iterable<Dom4jFluent.Element>>asList(
                        resourceDescriptors,
                        dependencies,
                        condition,
                        contexts
                )).toDom());
        descriptor.enabled();
        return descriptor;
    }
}
