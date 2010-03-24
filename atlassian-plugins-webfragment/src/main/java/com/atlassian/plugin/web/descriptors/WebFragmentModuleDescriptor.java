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
public interface WebFragmentModuleDescriptor extends ModuleDescriptor<Void>, WeightedDescriptor, StateAware, ConditionalDescriptor
{
    /**
     * @deprecated As of 2.5.0, use
     *             {@link ConditionElementParser.CompositeType#OR}
     */
    @Deprecated
    int COMPOSITE_TYPE_OR = ConditionElementParser.CompositeType.OR;

    /**
     * @deprecated As of 2.5.0, use
     *             {@link ConditionElementParser.CompositeType.AND}
     */
    @Deprecated
    int COMPOSITE_TYPE_AND = ConditionElementParser.CompositeType.AND;

    int getWeight();

    WebLabel getWebLabel();

    WebLabel getTooltip();

    Condition getCondition();

    ContextProvider getContextProvider();

    WebParam getWebParams();
}
