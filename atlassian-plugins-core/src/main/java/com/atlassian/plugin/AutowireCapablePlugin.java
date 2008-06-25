package com.atlassian.plugin;

/**
 * Defines a plugin that is capable of creating and autowiring beans.  The name and autowire types copied from Spring's
 * AutowireCapableBeanFactory.
 */
public interface AutowireCapablePlugin
{

    /**
     * The autowire strategy to use when creating and wiring a bean
     */
        /** Performs no autowiring */
        public static final int AUTOWIRE_NO = 1;

        /** Performs setter-based injection by name */
        public static final int AUTOWIRE_BY_NAME = 2;

        /** Performs setter-based injection by type */
        public static final int AUTOWIRE_BY_TYPE = 3;

        /** Performs construction-based injection by type */
        public static final int AUTOWIRE_BY_CONSTRUCTOR = 4;

        /**
         * Autodetects appropriate injection by first seeing if any no-arg constructors exist.  If not, performs constructor
         * injection, and if so, autowires by type then name
         */
        public static final int AUTOWIRE_AUTODETECT = 5;

    /**
     * Creates and autowires a class using the default strategy.
     * @param clazz The class to create
     * @return The created and wired bean
     */
    Object autowire(Class clazz);

    /**
     * Creates and autowires a class with a specific autowire strategy
     *
     * @param clazz The class to create
     * @param autowireStrategy The autowire strategy
     * @return The created and wired bean
     */
    Object autowire(Class clazz, int autowireStrategy);
}
