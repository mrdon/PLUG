package com.atlassian.plugin.osgi.factory.transform;

import junit.framework.TestCase;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;
import java.util.ArrayList;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

public class TestHostComponentSpringTransformer extends TestCase
{
    HostComponentSpringTransformer transformer = new HostComponentSpringTransformer();
    public void testTransform() throws IOException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        SpringTransformerTestHelper.transform(transformer, new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration("foo", String.class));
        }}, pluginRoot, "osgi:reference[@id='foo' and @filter='(&(bean-name=foo)(plugins-host=true))']/osgi:interfaces/beans:value/text()='java.lang.String'");
    }

    public void testTransformWithPoundSign() throws IOException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        SpringTransformerTestHelper.transform(transformer, new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration("foo#1", String.class));
        }}, pluginRoot, "osgi:reference[@id='fooLB1' and @filter='(&(bean-name=foo#1)(plugins-host=true))']/osgi:interfaces/beans:value/text()='java.lang.String'");
    }

    public void testTransformWithNoBeanName() throws IOException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        SpringTransformerTestHelper.transform(transformer, new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration(String.class));
        }}, pluginRoot, "osgi:reference[@id='bean0' and not(@filter)]/osgi:interfaces/beans:value/text()='java.lang.String'");
    }

    public void testTransformWithExistingComponentImport() throws IOException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element compImport = pluginRoot.addElement("component-import");
        compImport.addAttribute("key", "foo");
        SpringTransformerTestHelper.transform(transformer, new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration("foo", String.class));
        }}, pluginRoot, "osgi:reference[@id='foo0' and @filter='(&(bean-name=foo)(plugins-host=true))']/osgi:interfaces/beans:value/text()='java.lang.String'");
    }
}