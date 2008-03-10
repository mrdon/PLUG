package com.atlassian.plugin.servlet;

import javax.servlet.ServletContext;

import junit.framework.TestCase;

import com.atlassian.plugin.descriptors.servlet.ServletContextWrapper;
import com.atlassian.plugin.descriptors.servlet.ServletModuleDescriptor;
import com.atlassian.plugin.descriptors.servlet.ServletModuleManager;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

public class TestServletContextWrapper extends TestCase
{
    Mock mockServletContext;
    
    ServletContext contextWrapper;
    
    public void setUp()
    {
        mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getAttribute", C.eq("wrapped"), "wrapped value");
        
        ServletModuleDescriptor descriptor = new ServletModuleDescriptor()
        {
            protected void autowireObject(Object obj) {}
            protected ServletModuleManager getServletModuleManager() { return null; }
        };
        
        contextWrapper = new ServletContextWrapper(descriptor, (ServletContext) mockServletContext.proxy());
    }
    
    public void testPutAttribute()
    {
        // if set attribute is called on the wrapped context it will throw an 
        // exception since it is not expecting it
        contextWrapper.setAttribute("attr", "value");
        assertEquals("value", contextWrapper.getAttribute("attr"));
    }
    
    public void testGetAttributeDelegatesToWrappedContext()
    {
        assertEquals("wrapped value", contextWrapper.getAttribute("wrapped"));
    }

    public void testPutAttributeOverridesWrapperContextAttribute()
    {
        // if set attribute is called on the wrapped context it will throw an 
        // exception since it is not expecting it
        contextWrapper.setAttribute("wrapped", "value");
        assertEquals("value", contextWrapper.getAttribute("wrapped"));
    }    
}
