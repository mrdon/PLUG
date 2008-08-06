package com.atlassian.plugin.util;

import java.util.Set;
import java.util.HashSet;

public class ClassUtils
{
    private ClassUtils() {}

    public static Set<Class> findAllTypes(Class cls)
    {
        Set<Class> types = new HashSet<Class>();
        findAllTypes(cls, types);
        return types;
    }

    /**
     * Finds all super classes and interfaces for a given class
     * @param cls The class to scan
     * @param types The collected related classes found
     */
    public static void findAllTypes(Class cls, Set<Class> types)
    {
        if (cls == null)
            return;

        // check to ensure it hasn't been scanned yet
        if (types.contains(cls))
            return;

        types.add(cls);

        findAllTypes(cls.getSuperclass(), types);
        for (int x = 0; x<cls.getInterfaces().length; x++)
            findAllTypes(cls.getInterfaces()[x], types);
    }
}
