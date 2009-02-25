package com.atlassian.plugin.descriptors.servlet;

import junit.framework.TestCase;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletConfig;

import static org.mockito.Mockito.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class TestServletModuleContainerServlet extends TestCase
{
    private ServletModuleManager mockServletModuleManager;
    private HttpServlet mockServlet;
    private ServletModuleContainerServlet servletModuleContainerServlet;
    private MockHttpServletRequest mockRequest;
    private MockHttpServletResponse mockResponse;

    protected void setUp() throws Exception
    {
        super.setUp();
        mockServletModuleManager = mock(ServletModuleManager.class);
        servletModuleContainerServlet = new ServletModuleContainerServlet()
        {
            protected ServletModuleManager getServletModuleManager()
            {
                return mockServletModuleManager;
            }
        };
        mockServlet = mock(HttpServlet.class);
        mockRequest = new MockHttpServletRequest();
        mockResponse = new MockHttpServletResponse();
    }

    public void testNormalRequest() throws Exception
    {
        when(mockServletModuleManager.getServlet(anyString(), (ServletConfig) isNull())).thenReturn(mockServlet);
        mockRequest.setPathInfo("pathinfo");
        servletModuleContainerServlet.service(mockRequest, mockResponse);
        verify(mockServletModuleManager).getServlet("pathinfo", null);
        verify(mockServlet).service(mockRequest, mockResponse);
    }

    public void testDispatcherIncludeRequest() throws Exception
    {
        when(mockServletModuleManager.getServlet(anyString(), (ServletConfig) isNull())).thenReturn(mockServlet);
        mockRequest.setPathInfo("badpathinfo");
        mockRequest.setAttribute("javax.servlet.include.path_info", "pathinfo");
        servletModuleContainerServlet.service(mockRequest, mockResponse);
        verify(mockServletModuleManager).getServlet("pathinfo", null);
        verify(mockServlet).service(mockRequest, mockResponse);
    }

    public void testNormalNoServletFound() throws Exception
    {
        mockRequest.setRequestURI("normaluri");
        servletModuleContainerServlet.service(mockRequest, mockResponse);
        assertTrue(mockResponse.getErrorMessage().contains("normaluri"));
    }

    public void testDispatcherIncludeNoServletFound() throws Exception
    {
        mockRequest.setRequestURI("baduri");
        mockRequest.setAttribute("javax.servlet.include.request_uri", "includeuri");
        servletModuleContainerServlet.service(mockRequest, mockResponse);
        assertTrue(mockResponse.getErrorMessage().contains("includeuri"));
    }

}
