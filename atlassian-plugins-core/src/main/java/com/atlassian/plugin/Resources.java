package com.atlassian.plugin;

import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import org.dom4j.Element;

import java.util.*;

/**
 * An aggregate of all resource descriptors within the given plugin module or plugin.
 *
 * @see com.atlassian.plugin.impl.AbstractPlugin#resources
 * @see com.atlassian.plugin.descriptors.AbstractModuleDescriptor#resources
 */
public class Resources implements Resourced
{
    public static final Resources EMPTY_RESOURCES = new Resources(Collections.EMPTY_LIST);
    private List resourceDescriptors;

    /**
     * Parses the resource descriptors from the provided plugin XML element and creates a Resources object containing them.
     * <p/>
     * If the module or plugin contains no resource elements, an empty Resources object will be returned. This method will
     * not return null.
     *
     * @param element the plugin or plugin module XML fragment which should not be null
     * @return a Resources object representing the resources in the plugin or plugin module
     * @throws PluginParseException if there are two resources with the same name and type in this element, or another parse error
     * occurs
     * @throws IllegalArgumentException if the provided element is null
     */
    public static Resources fromXml(Element element) throws PluginParseException, IllegalArgumentException
    {
        if (element == null)
            throw new IllegalArgumentException("Cannot parse resources from null XML element");

        List elements = element.elements("resource");

        List templates = new ArrayList(elements.size());

        for (Iterator iterator = elements.iterator(); iterator.hasNext();)
        {
            final ResourceDescriptor resourceDescriptor = new ResourceDescriptor((Element) iterator.next());

            if (templates.contains(resourceDescriptor))
                throw new PluginParseException("Duplicate resource with type '" + resourceDescriptor.getType() + "' and name '" + resourceDescriptor.getName() + "' found");

            templates.add(resourceDescriptor);
        }

        return new Resources(templates);
    }

    /**
     * Create a resource object with the given resource descriptors. The provided list must not be null.
     *
     * @param resourceDescriptors the descriptors which are part of this resources object
     * @throws IllegalArgumentException if the resourceDescriptors list is null
     */
    public Resources(List resourceDescriptors) throws IllegalArgumentException
    {
        if (resourceDescriptors == null)
            throw new IllegalArgumentException("Resources cannot be created with a null resources list. Pass empty list instead");
        this.resourceDescriptors = resourceDescriptors;
    }

   public List getResourceDescriptors()
    {
        return resourceDescriptors;
    }

    public List getResourceDescriptors(String type)
    {
        List typedResourceDescriptors = new LinkedList();

        for (Iterator iterator = resourceDescriptors.iterator(); iterator.hasNext();)
        {
            ResourceDescriptor resourceDescriptor = (ResourceDescriptor) iterator.next();
            if (resourceDescriptor.getType().equalsIgnoreCase(type))
            {
                typedResourceDescriptors.add(resourceDescriptor);
            }
        }

        return typedResourceDescriptors;
    }

    public ResourceLocation getResourceLocation(String type, String name)
    {
        for (Iterator iterator = resourceDescriptors.iterator(); iterator.hasNext();)
        {
            ResourceDescriptor resourceDescriptor = (ResourceDescriptor) iterator.next();
            if (resourceDescriptor.doesTypeAndNameMatch(type, name))
            {
                return resourceDescriptor.getResourceLocationForName(name);
            }
        }

        return null;
    }

    /**
     * @deprecated
     */
    public ResourceDescriptor getResourceDescriptor(String type, String name)
    {
        for (Iterator iterator = resourceDescriptors.iterator(); iterator.hasNext();)
        {
            ResourceDescriptor resourceDescriptor = (ResourceDescriptor) iterator.next();
            if (resourceDescriptor.getType().equalsIgnoreCase(type) && resourceDescriptor.getName().equalsIgnoreCase(name))
            {
                return resourceDescriptor;
            }
        }

        return null;
    }
}
