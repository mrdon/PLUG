package com.atlassian.plugin.osgi.test;

import com.atlassian.plugin.osgi.Callable2;

public class Callable2Factory
{
    public Callable2 create()
    {
        return new Callable2()
        {

            public String call() throws Exception
            {
                return "called";
            }
        };
    }

    public Callable2 foo() {
        return null;
    }
}
