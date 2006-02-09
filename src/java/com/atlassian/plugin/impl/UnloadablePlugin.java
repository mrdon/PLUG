package com.atlassian.plugin.impl;

/**
 * Created by IntelliJ IDEA.
 * User: Jeremy Higgs
 * Date: 6/02/2006
 * Time: 16:18:46
 */

public class UnloadablePlugin extends StaticPlugin
{
    public boolean isEnabledByDefault()
    {
        return false;
    }
}
