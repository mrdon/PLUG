package com.atlassian.plugin.mock;

public class MockBear
{
    public int hashCode()
    {
        return 7;
    }

    public boolean equals(Object obj)
    {
        return obj instanceof MockBear;
    }
}
