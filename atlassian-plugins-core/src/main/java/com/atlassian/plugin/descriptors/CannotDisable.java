package com.atlassian.plugin.descriptors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marks {@link com.atlassian.plugin.ModuleDescriptor} implementations that cannot be disabled.
 * If this annotation is not present, it is assumed that the module descriptor
 * supports disablement.
 *
 * @since 2.5.0
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CannotDisable
{
}
