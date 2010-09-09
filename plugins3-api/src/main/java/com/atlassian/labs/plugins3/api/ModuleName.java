package com.atlassian.labs.plugins3.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleName
{
    String value();
}
