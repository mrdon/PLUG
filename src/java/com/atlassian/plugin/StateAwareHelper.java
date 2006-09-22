package com.atlassian.plugin;

/**
 * Created by IntelliJ IDEA.
 * User: Tomd
 * Date: 22/09/2006
 * Time: 10:53:36
 * To change this template use File | Settings | File Templates.
 */
public class StateAwareHelper implements StateAware
{
    private boolean enabled = false;
    public void enabled()
    {
        enabled = true;
    }

    public void disabled()
    {
        enabled = false;
    }

    public boolean isEnabled()
    {
        return enabled;
    }
}
