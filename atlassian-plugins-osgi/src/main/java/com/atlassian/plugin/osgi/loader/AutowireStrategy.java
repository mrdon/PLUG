package com.atlassian.plugin.osgi.loader;

import com.atlassian.plugin.AutowireCapablePlugin;


/**
 * How to autowire a component
 */
public enum AutowireStrategy {
    /**
     * Performs no autowiring
     */
    AUTOWIRE_NO(AutowireCapablePlugin.AUTOWIRE_NO),
    /**
     * Performs setter-based injection by name
     */
    AUTOWIRE_BY_NAME(AutowireCapablePlugin.AUTOWIRE_BY_NAME),

    /**
     * Performs setter-based injection by type
     */
    AUTOWIRE_BY_TYPE(AutowireCapablePlugin.AUTOWIRE_BY_TYPE),

    /**
     * Performs construction-based injection by type
     */
    AUTOWIRE_BY_CONSTRUCTOR(AutowireCapablePlugin.AUTOWIRE_BY_CONSTRUCTOR),

    /**
     * Autodetects appropriate injection by first seeing if any no-arg constructors exist.  If not, performs constructor
     * injection, and if so, autowires by type then name
     */
    AUTOWIRE_AUTODETECT(AutowireCapablePlugin.AUTOWIRE_AUTODETECT);

    private final int index;

    AutowireStrategy(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static AutowireStrategy fromIndex(int index) {
        for (AutowireStrategy s : AutowireStrategy.values()) {
            if (s.getIndex() == index) {
                return s;
            }
        }
        throw new IllegalArgumentException("The index " + index + " does not match any value of " + AutowireStrategy.class);
    }
}