package com.atlassian.plugin.servlet;

import junit.framework.TestCase;

import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;

import java.util.Enumeration;

import com.atlassian.plugin.servlet.util.ClassLoaderStack;

/**
 * @since 2.3.9
 */
public class TestPluginHttpSessionWrapper extends TestCase
{
    public void testGetAttribute() throws Exception
    {
        // Mock the Session
        MockSession mockSession = new MockSession(Thread.currentThread().getContextClassLoader());

        PluginHttpSessionWrapper sessionWrapper = new PluginHttpSessionWrapper(mockSession);

        // First try getSttribute() without a new ClassLoader
        sessionWrapper.getAttribute("foo");

        // Now put a different ClassLoader in the ContextClassLoader.
        ClassLoader pluginClassLoader = new ClassLoader(){};
        ClassLoaderStack.push(pluginClassLoader);
        try
        {
            // The MockSession will fail if called with the wrong ClassLoader
            sessionWrapper.getAttribute("foo");
            // PluginHttpSessionWrapper should have temporarily popped the ContextClassLoader, but now should have pushed
            // our MockClassLoader back
            assertSame(pluginClassLoader, Thread.currentThread().getContextClassLoader());
        }
        finally
        {
            ClassLoaderStack.pop();
        }
    }

    private class MockSession implements HttpSession
    {
        private final ClassLoader expectedClassLoader;

        public MockSession(final ClassLoader expectedClassLoader)
        {
            this.expectedClassLoader = expectedClassLoader;
        }

        public long getCreationTime()
        {
            return 0;
        }

        public String getId()
        {
            return null;
        }

        public long getLastAccessedTime()
        {
            return 0;
        }

        public ServletContext getServletContext()
        {
            return null;
        }

        public void setMaxInactiveInterval(final int interval)
        {
        }

        public int getMaxInactiveInterval()
        {
            return 0;
        }

        @SuppressWarnings ({ "deprecation" })
        public javax.servlet.http.HttpSessionContext getSessionContext()
        {
            return null;
        }

        public Object getAttribute(final String name)
        {
            // We just care that the context ClassLoader is correct
            assertSame(expectedClassLoader, Thread.currentThread().getContextClassLoader());

            return null;
        }

        public Object getValue(final String name)
        {
            return null;
        }

        public Enumeration getAttributeNames()
        {
            return null;
        }

        public String[] getValueNames()
        {
            return new String[0];
        }

        public void setAttribute(final String name, final Object value)
        {
        }

        public void putValue(final String name, final Object value)
        {
        }

        public void removeAttribute(final String name)
        {
        }

        public void removeValue(final String name)
        {
        }

        public void invalidate()
        {
        }

        public boolean isNew()
        {
            return false;
        }
    }
}