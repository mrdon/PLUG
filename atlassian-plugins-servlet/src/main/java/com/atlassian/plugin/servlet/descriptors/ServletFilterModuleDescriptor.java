package com.atlassian.plugin.servlet.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.HostContainerLegacyAdaptor;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.filter.FilterDispatcherCondition;
import com.atlassian.plugin.servlet.filter.FilterLocation;
import com.atlassian.plugin.util.validation.ValidationPattern;
import org.apache.commons.lang.Validate;
import org.dom4j.Element;

import javax.servlet.Filter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.atlassian.plugin.util.validation.ValidationPattern.test;

/**
 * A module descriptor that allows plugin developers to define servlet filters.  Developers can define what urls the 
 * filter should be applied to by defining one or more &lt;url-pattern&gt; elements and they can decide where in the
 * filter stack a plugin filter should go by defining the "location" and "weight" attributes. 
 * <p/>
 * The location attribute can have one of four values:
 * </p>
 * <ul>
 * <li>after-encoding - after the character encoding filter</li>
 * <li>before-login - before the login filter</li>
 * <li>before-decoration - before any global decoration like sitemesh</li>
 * <li>before-dispatch - before any dispatching filters or servlets</li>
 * </ul>
 * <p>
 * The default for the location attribute is "before-dispatch".
 * <p/>
 * The weight attribute can have any integer value.  Filters with lower values of the weight attribute will come before
 * those with higher values within the same location.
 *
 * @since 2.1.0
 */
public class ServletFilterModuleDescriptor extends BaseServletModuleDescriptor<Filter> implements StateAware
{
    static final String DEFAULT_LOCATION = FilterLocation.BEFORE_DISPATCH.name();
    static final String DEFAULT_WEIGHT = "100";

    private FilterLocation location;

    private int weight;
    private final ServletModuleManager servletModuleManager;

    private Set<FilterDispatcherCondition> dispatcherConditions = new HashSet<FilterDispatcherCondition>();

    /**
     * Creates a descriptor that uses a module class factory to create instances.
     *
     * @param moduleFactory The module factory
     * @param servletModuleManager The module manager
     * @since 2.5.0
     */
    public ServletFilterModuleDescriptor(ModuleFactory moduleFactory, ServletModuleManager servletModuleManager)
    {
        super(moduleFactory);
        Validate.notNull(servletModuleManager);
        this.servletModuleManager = servletModuleManager;
    }

    /**
     * Creates a descriptor that uses a module factory to create instances
     *
     * @param hostContainer the host application's dependency injection system.
     * @param servletModuleManager The module manager
     * @since 2.2.0
     * @deprecated use {@link com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptor#ServletFilterModuleDescriptor(com.atlassian.plugin.module.ModuleFactory , com.atlassian.plugin.servlet.ServletModuleManager)} instead
     */
    public ServletFilterModuleDescriptor(HostContainer hostContainer, ServletModuleManager servletModuleManager)
    {
        this (new HostContainerLegacyAdaptor(hostContainer), servletModuleManager);
    }

    public static final Comparator<ServletFilterModuleDescriptor> byWeight = new Comparator<ServletFilterModuleDescriptor>()
    {
        public int compare(ServletFilterModuleDescriptor lhs, ServletFilterModuleDescriptor rhs)
        {
            return Integer.valueOf(lhs.getWeight()).compareTo(rhs.getWeight());
        }
    };

    @SuppressWarnings("unchecked")
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        try
        {
            location = FilterLocation.parse(element.attributeValue("location", DEFAULT_LOCATION));
            weight = Integer.valueOf(element.attributeValue("weight", DEFAULT_WEIGHT));
        }
        catch (IllegalArgumentException ex)
        {
            throw new PluginParseException(ex);
        }

        dispatcherConditions.clear();
        List<Element> dispatcherElements = element.elements("dispatcher");
        for (Element dispatcher : dispatcherElements)
        {
            // already been validated via the validation rules
            dispatcherConditions.add(FilterDispatcherCondition.valueOf(dispatcher.getTextTrim()));
        }
    }

    @Override
    protected void provideValidationRules(ValidationPattern pattern) {
        super.provideValidationRules(pattern);
        StringBuilder conditionRule = new StringBuilder();
        conditionRule.append("dispatcher[");
        for (int x = 0; x < FilterDispatcherCondition.values().length; x++)
        {
            conditionRule.append(". != '").append(FilterDispatcherCondition.values()[x]).append("'");
            if (x + 1 < FilterDispatcherCondition.values().length)
            {
                conditionRule.append(" and ");
            }
        }
        conditionRule.append("]");
        pattern.rule(conditionRule.toString(), test("dispatcher")
                .withError("The dispatcher value must be one of the following only " + Arrays.asList(FilterDispatcherCondition.values())));
    }

    public void enabled()
    {
        super.enabled();
        servletModuleManager.addFilterModule(this);
    }

    public void disabled()
    {
        servletModuleManager.removeFilterModule(this);
        super.disabled();
    }

    @Override
    public Filter getModule()
    {
        return moduleFactory.createModule(moduleClassName, this);
    }

    public FilterLocation getLocation()
    {
        return location;
    }

    public int getWeight()
    {
        return weight;
    }

    /**
     * Returns a set of dispatcher conditions that have been set for this filter, these conditions
     * will be one of the following: <code>REQUEST, FORWARD, INCLUDE or ERROR</code>.
     *
     * @return A set of dispatcher conditions that have been set for this filter.
     * @since 2.5.0
     */
    public Set<FilterDispatcherCondition> getDispatcherConditions()
    {
        return dispatcherConditions;
    }
}
