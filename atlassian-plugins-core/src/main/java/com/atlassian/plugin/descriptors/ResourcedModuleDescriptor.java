package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.loaders.LoaderUtils;
import org.dom4j.Element;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

/**
 * @deprecated All module descriptors now have resources. Use AbstractModuleDescriptor instead.
 */
public abstract class ResourcedModuleDescriptor extends AbstractModuleDescriptor
{
}
