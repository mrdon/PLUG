package com.atlassian.plugin.osgi;

import com.atlassian.plugin.util.WaitUntil;

public abstract class AbstractWaitCondition implements WaitUntil.WaitCondition
{
    public abstract boolean isFinished();

    public String getWaitMessage()
    {
        return "Waiting for test condition to be true";
    }
}
