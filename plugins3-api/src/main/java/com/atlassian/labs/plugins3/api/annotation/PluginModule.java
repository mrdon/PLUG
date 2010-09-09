package com.atlassian.labs.plugins3.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The base annotation for all plugin modules
 *
 * @see org.springframework.stereotype.Component
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginModule
{
    /**
	 * @return The plugin module key
	 */
	String value() default "";

}
