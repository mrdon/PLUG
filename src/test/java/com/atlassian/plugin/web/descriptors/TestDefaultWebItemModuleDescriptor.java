package com.atlassian.plugin.web.descriptors;

import junit.framework.TestCase;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.DocumentException;
import com.atlassian.plugin.PluginParseException;

public class TestDefaultWebItemModuleDescriptor extends TestCase
{
    private WebItemModuleDescriptor descriptor;

    protected void setUp() throws Exception
    {
        descriptor = new DefaultWebItemModuleDescriptor(new MockWebInterfaceManager());
    }

    public void testGetStyleClass() throws DocumentException, PluginParseException
    {
        String className = "testClass";
        String styleClass = "<styleClass>"+className+"</styleClass>";

        Element element = createElement(styleClass);
        descriptor.init(null, element);

        assertEquals(className, descriptor.getStyleClass());
    }

    public void testGetStyleClassTrimmed() throws DocumentException, PluginParseException
    {
        String className = "testClass";
        String styleClass = "<styleClass>   "+className+"   </styleClass>";

        Element element = createElement(styleClass);
        descriptor.init(null, element);

        assertEquals(className, descriptor.getStyleClass());
    }

    public void testGetStyleClassSpaceSeparated() throws DocumentException, PluginParseException
    {
        String className = "testClass testClass2";
        String styleClass = "<styleClass>"+className+"</styleClass>";

        Element element = createElement(styleClass);
        descriptor.init(null, element);

        assertEquals(className, descriptor.getStyleClass());
    }

    public void testGetStyleClassEmpty() throws DocumentException, PluginParseException
    {
        String styleClass = "<styleClass></styleClass>";

        Element element = createElement(styleClass);
        descriptor.init(null, element);

        assertEquals("", descriptor.getStyleClass());
    }

    public void testGetStyleClassNone() throws DocumentException, PluginParseException
    {
        String styleClass = "";

        Element element = createElement(styleClass);
        descriptor.init(null, element);

        assertEquals("", descriptor.getStyleClass());   // should not be null!
    }

    private Element createElement(String childElement) throws DocumentException
    {
        String rootElement = "<root>"+childElement+"</root>";
        Document document = DocumentHelper.parseText(rootElement);
        Element element = document.getRootElement();
        return element;
    }
}
