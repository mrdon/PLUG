package com.atlassian.labs.plugins3.extender;

import com.atlassian.labs.plugins3.api.ApplicationInfo;

/**
 *
 */
public class DummyApplicationInfo implements ApplicationInfo
{
    public String getVersion()
    {
        return "1.0";
    }

    public long getBuildNumber()
    {
        return 100;
    }

    public String getType()
    {
        return REFAPP;
    }
}
