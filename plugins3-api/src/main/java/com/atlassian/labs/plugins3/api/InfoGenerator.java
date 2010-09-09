package com.atlassian.labs.plugins3.api;

/**
 *
 */
public interface InfoGenerator<M extends InfoGenerator>
{
    M name(String name);
    M description(String description);
    M descriptionI18nKey(String descriptionKey);
    M version(String version);
    M vendorName(String vendorName);
    M vendorUrl(String vendorUrl);
    M addParameter(String key, String value);
}
