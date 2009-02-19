package com.atlassian.plugin.osgi.factory.transform.stage;

import junit.framework.TestCase;
import org.dom4j.Element;
import org.dom4j.DocumentHelper;
import org.dom4j.DocumentException;

import java.io.IOException;

import com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;

public class TestModuleTypeSpringStage extends TestCase
{
    public void testTransform() throws IOException, DocumentException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");
        moduleType.addAttribute("class", "my.FooDescriptor");

        SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                "beans:bean[@id='springHostContainer' and @class='"+ ModuleTypeSpringStage.SPRING_HOST_CONTAINER+"']",
                "beans:bean[@id='moduleType-foo' and @class='"+ SingleModuleDescriptorFactory.class.getName()+"']",
                "osgi:service[@id='moduleType-foo_osgiService' and @auto-export='interfaces']");
    }

    public void testTransformOfBadElement() throws IOException, DocumentException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");

        try
        {
            SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                    "beans:bean[@id='moduleType-foo' and @class='"+ SingleModuleDescriptorFactory.class.getName()+"']",
                    "osgi:service[@id='moduleType-foo_osgiService' and @auto-export='interfaces']");
            fail();
        }
        catch (PluginParseException ex)
        {
            // pass
        }
    }

    public void testTransformOfBadElementKey() throws IOException, DocumentException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element moduleType = pluginRoot.addElement("module-type");

        try
        {
            SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                    "beans:bean[@id='moduleType-foo' and @class='"+ SingleModuleDescriptorFactory.class.getName()+"']",
                    "osgi:service[@id='moduleType-foo_osgiService' and @auto-export='interfaces']");
            fail();
        }
        catch (PluginParseException ex)
        {
            // pass
        }
    }

}
