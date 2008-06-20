package com.atlassian.plugin.descriptors.servlet;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * A wrapper around servlet context that allows plugin servlets to add
 * attributes which will not be shared/clobbered by other plugins.
 */
public class PluginServletContextWrapper implements ServletContext
{
    private final ServletModuleDescriptor descriptor;
    private final ServletContext context;
    private final ConcurrentMap attributes;
    private final Method methodGetContextPath;
    
    public PluginServletContextWrapper(ServletModuleDescriptor descriptor, ServletContext context)
    {
        Method tmpMethod = null;
        this.descriptor = descriptor;
        this.context = context;
        this.attributes = new ConcurrentHashMap();

        Class cls = context.getClass();
        try
        {
            tmpMethod = cls.getMethod("getContextPath");
        } catch (NoSuchMethodException e)
        {
            // no problem, Servlet 2.4 or earlier found
        }
        methodGetContextPath = tmpMethod;
    }

    /**
     * <p>Gets the named attribute.  The attribute is first looked for in the local
     * attribute map, if it is not found there it is looked for in the wrapped
     * contexts attribute map.  If it is not there, null is returned.</p>
     * 
     * <p>A consequence of this ordering is that servlets may, in their own
     * context, override but not overwrite attributes from the wrapped context.</p>
     */
    public Object getAttribute(String name)
    {
        Object attr = attributes.get(name);
        if (attr == null)
            attr = context.getAttribute(name);

        return attr;
    }

    /**
     * @return an enumeration of all the attributes from the wrapped 
     *         context as well as the local attributes.
     */
    public Enumeration getAttributeNames()
    {
        Collection names = new HashSet();
        names.addAll(attributes.keySet());
        names.addAll(Collections.list(context.getAttributeNames()));
        return Collections.enumeration(names);
    }

    /**
     * Removes an attribute from the local context.  Leaves the wrapped context
     * completely untouched.
     */
    public void removeAttribute(String name)
    {
        attributes.remove(name);
    }

    /**
     * <p>Sets an attribute in the local attribute map, leaving the wrapped
     * context untouched.</p>
     * 
     * <p>Servlets may use this and the lookup ordering of the
     * <code>getAttribute()</code> method to effectively override the value
     * of an attribute in the wrapped servlet context with their own value and 
     * this overridden value will only be seen in the plugins own scope.</p>
     */
    public void setAttribute(String name, Object object)
    {
        attributes.put(name, object);
    }

    /**
     * @return the init parameter from the servlet module descriptor.
     */
    public String getInitParameter(String name)
    {
        return (String) descriptor.getInitParams().get(name);
    }

    /**
     * @return an enumeration of the init parameters from the servlet module
     * descriptor.
     */
    public Enumeration getInitParameterNames()
    {
        return Collections.enumeration(descriptor.getInitParams().keySet());
    }

    /**
     * @return the resource from the plugin classloader if it exists, otherwise the 
     *         resource is looked up from the wrapped context and returned
     */
    public URL getResource(String path) throws MalformedURLException
    {
        URL url = descriptor.getPlugin().getResource(path);
        if (url == null)
        {
            url = context.getResource(path);
        }
        return url;
    }

    /**
     * @return the resource stream from the plugin classloader if it exists, otherwise
     *         the resource stream is attempted to be retrieved from the wrapped context
     */
    public InputStream getResourceAsStream(String path)
    {
        InputStream in = descriptor.getPlugin().getResourceAsStream(path);
        if (in == null)
        {
            in = context.getResourceAsStream(path);
        }
        return in;
    }

    /**
     * @return null so that servlet plugins can't escape their box
     */
    public ServletContext getContext(String uripath)
    {
        return null;
    }

    //---- All methods below simply delegate to the wrapped servlet context ----

    public String getContextPath() {

        // all this crap to deal with Servlet 2.4 containers better
        if (methodGetContextPath != null)
        {
            try
            {
                return (String) methodGetContextPath.invoke(context);
            } catch (IllegalAccessException e)
            {
                throw new RuntimeException("Cannot access this method", e);
            } catch (InvocationTargetException e)
            {
                throw new RuntimeException("Unable to execute getContextPath()", e.getCause());
            }
        } else
        {
            throw new UnsupportedOperationException("This servlet context doesn't support 2.5 methods");
        }

    }

    public int getMajorVersion()
    {
        return context.getMajorVersion();
    }

    public String getMimeType(String file)
    {
        return context.getMimeType(file);
    }

    public int getMinorVersion()
    {
        return context.getMinorVersion();
    }

    public RequestDispatcher getNamedDispatcher(String name)
    {
        return context.getNamedDispatcher(name);
    }

    public String getRealPath(String path)
    {
        return context.getRealPath(path);
    }

    public RequestDispatcher getRequestDispatcher(String path)
    {
        return context.getRequestDispatcher(path);
    }

    public Set getResourcePaths(String arg0)
    {
        return context.getResourcePaths(arg0);
    }

    public String getServerInfo()
    {
        return context.getServerInfo();
    }

    public Servlet getServlet(String name) throws ServletException
    {
        return context.getServlet(name);
    }

    public String getServletContextName()
    {
        return context.getServletContextName();
    }

    public Enumeration getServletNames()
    {
        return context.getServletNames();
    }

    public Enumeration getServlets()
    {
        return context.getServlets();
    }

    public void log(Exception exception, String msg)
    {
        context.log(exception, msg);
    }

    public void log(String message, Throwable throwable)
    {
        context.log(message, throwable);
    }

    public void log(String msg)
    {
        context.log(msg);
    }

}
