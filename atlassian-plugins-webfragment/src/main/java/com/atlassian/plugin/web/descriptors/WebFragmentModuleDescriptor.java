package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.model.WebLabel;
import com.atlassian.plugin.web.model.WebParam;

/**
 * A convenience interface for web fragment descriptors
 */
public interface WebFragmentModuleDescriptor<T> extends ModuleDescriptor<T>, WeightedDescriptor, StateAware, ConditionalDescriptor
{
    /**
     * @deprecated As of 2.5.0, use {@link ConditionElementParser#COMPOSITE_TYPE_OR}
     */
    int COMPOSITE_TYPE_OR = 0;

    /**
     * @deprecated As of 2.5.0, use {@link ConditionElementParser#COMPOSITE_TYPE_AND}
     */
    int COMPOSITE_TYPE_AND = 1;

    int getWeight();

    WebLabel getWebLabel();

    WebLabel getTooltip();

    Condition getCondition();

    ContextProvider getContextProvider();

    WebParam getWebParams();
}
