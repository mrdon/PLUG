/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 3:59:16 PM
 */
package com.atlassian.plugin.mock;

import com.atlassian.plugin.descriptors.ResourceParameterGenerator;

import java.util.Map;
import java.util.HashMap;

public class MockResourceParameterGenerator implements ResourceParameterGenerator
{
    String foo;
    public Map generateParameters()
    {
        Map params = new HashMap();
        params.put("foo", "bar");
        return params;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof MockResourceParameterGenerator)) return false;
        return true;
    }

    public int hashCode()
    {
        return 12;
    }
}