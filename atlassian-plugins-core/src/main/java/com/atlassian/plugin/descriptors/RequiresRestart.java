package com.atlassian.plugin.descriptors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks {@link com.atlassian.plugin.ModuleDescriptor} implementations that require a restart of the application to
 * start the plugin when installed at runtime.  If this annotation is not present, it is assumed that the module descriptor
 * supports runtime installation.
 *
 * @since 2.1
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresRestart
{
}
