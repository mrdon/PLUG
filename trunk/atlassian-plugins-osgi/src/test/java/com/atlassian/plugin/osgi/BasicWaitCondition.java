package com.atlassian.plugin.osgi;

import com.atlassian.plugin.util.WaitUntil;

public abstract class BasicWaitCondition implements WaitUntil.WaitCondition
{
    public abstract boolean isFinished();

    public String getWaitMessage()
    {
        return "";
    }
}
