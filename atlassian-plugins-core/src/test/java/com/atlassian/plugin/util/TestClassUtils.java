package com.atlassian.plugin.util;

import junit.framework.TestCase;

import java.io.Serializable;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

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

    public void testGetTypeArguments()
    {
        assertEquals(asList(String.class), ClassUtils.getTypeArguments(BaseClass.class, Child.class));

        assertEquals(asList(String.class), ClassUtils.getTypeArguments(BaseClass.class, Baby.class));

        assertEquals(singletonList(null), ClassUtils.getTypeArguments(BaseClass.class, ForgotType.class));
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
}
