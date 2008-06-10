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
    enum AutowireStrategy {
        /** Performs no autowiring */
        AUTOWIRE_NO,

        /** Performs setter-based injection by name */
        AUTOWIRE_BY_NAME,

        /** Performs setter-based injection by type */
        AUTOWIRE_BY_TYPE,

        /** Performs construction-based injection by type */
        AUTOWIRE_BY_CONSTRUCTOR,

        /**
         * Autodetects appropriate injection by first seeing if any no-arg constructors exist.  If not, performs constructor
         * injection, and if so, autowires by type then name
         */
        AUTOWIRE_AUTODETECT
    }

    /**
     * Creates and autowires a class.  The stategy defaults to AUTOWIRE_AUTODETECT.
     * @param clazz The class to create
     * @return The created and wired bean
     */
    <T> T autowire(Class<T> clazz);

    /**
     * Creates and autowires a class with a specific autowire strategy
     *
     * @param clazz The class to create
     * @param autowireStrategy The autowire strategy
     * @return The created and wired bean
     */
    <T> T autowire(Class<T> clazz, AutowireStrategy autowireStrategy);
}
