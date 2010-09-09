package com.atlassian.labs.plugins3.api;

import com.atlassian.labs.plugins3.api.module.helper.ResourceGenerator;

/**
 *
 */
public interface ModuleGenerator<T extends ModuleGenerator>
{
    T name(String name);
    T nameI18nKey(String i18nNameKey);
    T description(String description);
    T descriptionI18nKey(String descriptionKey);
    T addParameter(String key, String value);
    T enabledByDefault(boolean enabledByDefault);
    T addResource(ResourceGenerator resourceGenerator);
}
