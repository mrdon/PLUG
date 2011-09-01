package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.osgi.factory.transform.test.SomeClass;

public interface Barable
{
    // NOTE: this Integer parameter is used to verify that java.* packages are not imported/exported.
    SomeClass getSomeClass(Integer blah);
}
