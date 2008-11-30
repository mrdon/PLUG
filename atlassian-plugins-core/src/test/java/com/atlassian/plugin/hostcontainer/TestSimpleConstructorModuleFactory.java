package com.atlassian.plugin.hostcontainer;

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class TestSimpleConstructorModuleFactory extends TestCase
{
    public void testCreateModule()
    {
        Map<Class, Object> context = new HashMap<Class,Object>()
        {{
            put(String.class, "bob");
        }};

        SimpleConstructorHostContainer factory = new SimpleConstructorHostContainer(context);
        Base world = factory.create(OneArg.class);
        assertEquals("bob", world.getName());
    }

    public void testCreateModuleFindBiggest()
    {
        Map<Class, Object> context = new HashMap<Class,Object>()
        {{
            put(String.class, "bob");
            put(Integer.class, 10);
        }};

        SimpleConstructorHostContainer factory = new SimpleConstructorHostContainer(context);
        Base world = factory.create(TwoArg.class);
        assertEquals("bob 10", world.getName());
    }

    public void testCreateModuleFindSmaller()
    {
        Map<Class, Object> context = new HashMap<Class,Object>()
        {{
            put(String.class, "bob");
        }};

        SimpleConstructorHostContainer factory = new SimpleConstructorHostContainer(context);
        Base world = factory.create(TwoArg.class);
        assertEquals("bob", world.getName());
    }

    public void testCreateModuleNoMatch()
    {
        SimpleConstructorHostContainer factory = new SimpleConstructorHostContainer(Collections.<Class, Object>emptyMap());
        try
        {
            factory.create(OneArg.class);
            fail("Should have thrown exception");
        }
        catch (IllegalArgumentException ex)
        {
            // good, good
        }
    }

    public void testGetInstance()
    {
        Map<Class, Object> context = new HashMap<Class,Object>()
        {{
            put(String.class, "bob");
        }};

        SimpleConstructorHostContainer factory = new SimpleConstructorHostContainer(context);
        assertEquals("bob", factory.getInstance(String.class));
        assertNull(factory.getInstance(Integer.class));
    }

    public abstract static class Base
    {
        private final String name;
        public Base(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }
    }
    public static class OneArg extends Base
    {
        public OneArg(String name)
        {
            super(name);
        }
    }

    public static class TwoArg extends Base
    {
        public TwoArg(String name)
        {
            super(name);
        }

        public TwoArg(String name, Integer age)
        {
            super(name+" "+age);
        }
    }

}
