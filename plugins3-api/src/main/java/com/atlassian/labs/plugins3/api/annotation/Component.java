package com.atlassian.labs.plugins3.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an annotated class is a "component".
 * Such classes are considered as candidates for auto-detection
 * when using annotation-based configuration and classpath scanning.
 *
 * @see org.springframework.stereotype.Component
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PluginModule
public @interface Component
{
    /**
	 * @return the suggested component name, if any
	 */
	String value() default "";

    boolean isPublic() default false;

    Class<?>[] interfaces() default {};
}
