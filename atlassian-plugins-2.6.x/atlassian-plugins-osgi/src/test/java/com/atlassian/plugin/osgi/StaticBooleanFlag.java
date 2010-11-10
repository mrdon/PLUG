package com.atlassian.plugin.osgi;

import java.util.concurrent.atomic.AtomicBoolean;

public class StaticBooleanFlag
{
    public static final AtomicBoolean flag = new AtomicBoolean();
}
