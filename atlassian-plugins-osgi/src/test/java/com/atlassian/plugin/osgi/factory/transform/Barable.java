package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.osgi.factory.transform.test.SomeClass;

public interface Barable
{
    @DummyAnnotation SomeClass getSomeClass(Integer blah);
}
