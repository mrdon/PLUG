package com.atlassian.plugin.event.impl;

import junit.framework.TestCase;
import com.atlassian.plugin.event.impl.PluginEventManagerImpl;
import com.atlassian.plugin.event.impl.ListenerMethodSelector;
import com.atlassian.plugin.event.impl.MethodNameListenerMethodSelector;

import java.lang.reflect.Method;

public class TestPluginEventManagerImpl extends TestCase
{
    private PluginEventManagerImpl eventManager;

    public void setUp()
    {
        eventManager = new PluginEventManagerImpl();
    }

    public void tearDown()
    {
        eventManager = null;
    }
    public void testRegister()
    {
        Foo foo = new Foo();
        eventManager.register(foo);
        eventManager.broadcast(new Object());
        assertEquals(1, foo.called);
    }

    public void testRegisterWithBroadcastSupertype()
    {
        Foo foo = new Foo();
        eventManager.register(foo);
        eventManager.broadcast(new String());
        assertEquals(1, foo.called);
    }

    public void testRegisterWithFooBroadcastSupertype()
    {
        Foo foo = new Foo();
        eventManager.register(foo);
        eventManager.broadcast(new Foo());
        assertEquals(1, foo.fooCalled);
        assertEquals(1, foo.called);
    }

    public void testRegisterTwice()
    {
        Foo foo = new Foo();
        eventManager.register(foo);
        eventManager.register(foo);
        eventManager.broadcast(new Object());
        assertEquals(1, foo.called);
    }

    public void testRegisterWithBadListener()
    {
        BadListener l = new BadListener();
        eventManager.register(l);
        assertEquals(0, l.called);
    }

    public void testRegisterWithCustomSelector()
    {
        eventManager = new PluginEventManagerImpl(new ListenerMethodSelector[]{
                new ListenerMethodSelector() {
                    public boolean isListenerMethod(Method method)
                    {
                        return "onEvent".equals(method.getName());
                    }
                }
        });
        Foo foo = new Foo();
        eventManager.register(foo);
        eventManager.broadcast("jim");
        assertEquals(1, foo.jimCalled);
    }

    public void testRegisterWithOverlappingSelectors()
    {
        eventManager = new PluginEventManagerImpl(new ListenerMethodSelector[]{
                new MethodNameListenerMethodSelector(), new MethodNameListenerMethodSelector()});
        Foo foo = new Foo();
        eventManager.register(foo);
        eventManager.broadcast(new Object());
        assertEquals(1, foo.called);
    }

    public void testRegisterWithCustom()
    {
        Foo foo = new Foo();
        eventManager.register(foo);
        eventManager.broadcast(new Object());
        assertEquals(1, foo.called);
    }

    public void testUnregister()
    {
        Foo foo = new Foo();
        eventManager.register(foo);
        eventManager.broadcast(new Object());
        eventManager.unregister(foo);
        eventManager.broadcast(new Object());
        assertEquals(1, foo.called);
    }

    public void testSuperEvent()
    {
        Foo foo = new Foo();
        eventManager.register(foo);
        eventManager.broadcast(new Foo());
        assertEquals(1, foo.called);
    }

    public void testRegisterNull()
    {
        try
        {
            eventManager.register(null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException ex)
        {
            // test passed
        }
    }

    public static class Foo
    {
        int called = 0;
        int fooCalled = 0;
        int jimCalled = 0;

        public void channel(Object obj)
        {
            called++;
        }

        public void channel(Foo obj)
        {
            fooCalled++;
        }

        public void onEvent(String o)
        {
            jimCalled++;
        }
    }

    public static class BadListener
    {
        int called = 0;
        public void somemethod() {
            called++;
        }
    }



}
