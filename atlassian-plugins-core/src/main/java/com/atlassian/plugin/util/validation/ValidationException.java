package com.atlassian.plugin.util.validation;

import com.atlassian.plugin.PluginParseException;

import java.util.List;

/**
 * Exception for a validation error parsing DOM4J nodes
 *
 * @since 2.2.0
 */
public class ValidationException extends PluginParseException
{
    private final List<String> errors;
    public ValidationException(String msg, List<String> errors)
    {
        super(msg);
        this.errors = errors;
    }

    /**
     * @return a list of the original errors
     */
    public List<String> getErrors()
    {
        return errors;
    }
}
