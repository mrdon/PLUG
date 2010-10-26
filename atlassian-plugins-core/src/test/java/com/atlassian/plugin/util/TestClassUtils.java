package com.atlassian.plugin.util;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class TestClassUtils extends TestCase
{
    public void testFindAllTypes()
    {
        assertEquals(newHashSet(
                List.class,
                AbstractList.class,
                Cloneable.class,
                RandomAccess.class,
                AbstractCollection.class,
                Iterable.class,
                Collection.class,
                ArrayList.class,
                Object.class,
                Serializable.class
        ), ClassUtils.findAllTypes(ArrayList.class));
    }

    public void testFindParentTypesNoBoundary()
    {
        assertEquals(newHashSet(
                List.class,
                AbstractList.class,
                Cloneable.class,
                RandomAccess.class,
                AbstractCollection.class,
                Iterable.class,
                Collection.class,
                ArrayList.class,
                Object.class,
                Serializable.class
        ), ClassUtils.findAllTypesWithBoundary(ArrayList.class));
    }

    public void testFindParentTypes()
    {
        assertEquals(newHashSet(
                Iterable.class,
                Collection.class,
                Object.class,
                AbstractCollection.class
        ), ClassUtils.findAllTypesWithBoundary(AbstractCollection.class, Iterable.class));
    }

    public void testGetTypeArguments()
    {
        assertEquals(asList(String.class), ClassUtils.getTypeArguments(BaseClass.class, Child.class));

        assertEquals(asList(String.class), ClassUtils.getTypeArguments(BaseClass.class, Baby.class));

        assertEquals(singletonList(null), ClassUtils.getTypeArguments(BaseClass.class, ForgotType.class));
    }

    public void testGetTypeArgumentsDifferentClassloader() throws Exception
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try
        {
            URL log4jUrl = getClass().getClassLoader().getResource("log4j.properties");
            URL root = new URL(log4jUrl.toExternalForm() + "/../");

            URLClassLoader urlCl = new URLClassLoader(new URL[] {root}, new FilteredClassLoader(MySuperClass.class));
            ClosableClassLoader wrapCl = new ClosableClassLoader(getClass().getClassLoader());

            Thread.currentThread().setContextClassLoader(null);
            Class<?> module = ClassUtils.getTypeArguments((Class<Object>) urlCl.loadClass(MySuperClass.class.getName()),
                    (Class<? extends MySuperClass>) urlCl.loadClass(MySubClass.class.getName())).get(0);
            assertEquals(MyModule.class.getName(), module.getName());

            ClassLoader urlCl2 = new URLClassLoader(new URL[] {root}, new FilteredClassLoader(MySuperClass.class));
            assertTrue(wrapCl.loadClass(MySuperClass.class.getName()) == urlCl2.loadClass(MySuperClass.class.getName()));
            assertTrue(wrapCl.loadClass(MySubClass.class.getName()) != urlCl2.loadClass(MySubClass.class.getName()));
            assertTrue(wrapCl.loadClass(MySubClass.class.getName()).getSuperclass() == urlCl2.loadClass(MySubClass.class.getName()).getSuperclass());
            
            wrapCl.setClosed(true);
            //Thread.currentThread().setContextClassLoader(urlCl2);
            Class<?> module2 = ClassUtils.getTypeArguments((Class<Object>) urlCl2.loadClass(MySuperClass.class.getName()),
                    (Class<? extends MySuperClass>) urlCl2.loadClass(MySubClass.class.getName())).get(0);
            assertEquals(MyModule.class.getName(), module2.getName());
            assertTrue(module != module2);
            assertTrue(module != MyModule.class);
            assertTrue(module2 != MyModule.class);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public void testGetTypeArgumentsChildNotSubclass()
    {
        Class fakeChild = BaseClass.class;
        try
        {
            assertEquals(singletonList(null), ClassUtils.getTypeArguments(Baby.class, (Class<? extends Baby>) fakeChild));
            fail("Should have failed");
        }
        catch (IllegalArgumentException ex)
        {
            // this is good
        }
    }

    private static class BaseClass<T> {}
    private static class Child extends BaseClass<String>{}
    private static class ForgotType extends BaseClass{}

    private static class Mom<T> extends BaseClass<T>{}
    private static class Baby extends Mom<String>{}

    private static class ClosableClassLoader extends ClassLoader
    {
        private final ClassLoader delegate;
        private volatile boolean closed;

        public ClosableClassLoader(ClassLoader delegate)
        {
            super(null);
            this.delegate = delegate;
        }

        @Override
        public Class<?> loadClass(String name)
                throws ClassNotFoundException
        {
            checkClosed();
            return delegate.loadClass(name);
        }
        @Override
        public Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException
        {
            checkClosed();
            return delegate.loadClass(name);
        }

        private void checkClosed()
        {
            if (closed)
            {
                throw new IllegalStateException("Closed");
            }
        }

        @Override
        public URL getResource(String name)
        {
            checkClosed();
            return delegate.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name)
                throws IOException
        {
            checkClosed();
            return delegate.getResources(name);
        }

        public void setClosed(boolean closed)
        {
            this.closed = closed;
        }
    }

    private static class FilteredClassLoader extends ClassLoader
    {
        private final Collection<Class> classes;

        public FilteredClassLoader(Class... classes)
        {
            super(null);
            this.classes = asList(classes);
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException
        {
            for (Class cls : classes)
            {
                if (cls.getName().equals(name))
                {
                    return cls;
                }
            }
            if (name.startsWith("java."))
            {
                return ClassLoader.getSystemClassLoader().loadClass(name);
            }
            throw new ClassNotFoundException(name);
        }
    }
}
