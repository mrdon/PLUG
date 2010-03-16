package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.web.Condition;

/**
 * @since   2.5.0
 */
public interface ConditionalDescriptor
{
    Condition getCondition();
}
