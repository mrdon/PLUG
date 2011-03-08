package com.atlassian.plugin.osgi.spring;

import com.atlassian.plugin.osgi.spring.external.ApplicationContextPreProcessor;
import junit.framework.TestCase;
import org.osgi.framework.Bundle;
import org.springframework.osgi.extender.support.ApplicationContextConfiguration;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TestNonValidatingOsgiApplicationContextCreator extends TestCase
{
    public void testIsSpringPoweredNormal()
    {
        ApplicationContextConfiguration config = mock(ApplicationContextConfiguration.class);
        Bundle bundle = mock(Bundle.class);

        NonValidatingOsgiApplicationContextCreator creator = new NonValidatingOsgiApplicationContextCreator(Collections.<ApplicationContextPreProcessor>emptyList());
        when(config.isSpringPoweredBundle()).thenReturn(true);
        assertTrue(creator.isSpringPoweredBundle(bundle, config));
    }

    public void testIsSpringPoweredFalseNoProcessors()
    {
        ApplicationContextConfiguration config = mock(ApplicationContextConfiguration.class);
        Bundle bundle = mock(Bundle.class);

        NonValidatingOsgiApplicationContextCreator creator = new NonValidatingOsgiApplicationContextCreator(Collections.<ApplicationContextPreProcessor>emptyList());
        when(config.isSpringPoweredBundle()).thenReturn(false);
        assertFalse(creator.isSpringPoweredBundle(bundle, config));
    }

    public void testIsSpringPoweredFromPreProcessor()
    {
        ApplicationContextConfiguration config = mock(ApplicationContextConfiguration.class);
        Bundle bundle = mock(Bundle.class);
        ApplicationContextPreProcessor processor = mock(ApplicationContextPreProcessor.class);

        NonValidatingOsgiApplicationContextCreator creator = new NonValidatingOsgiApplicationContextCreator(Arrays.asList(processor));
        when(config.isSpringPoweredBundle()).thenReturn(false);
        when(processor.isSpringPoweredBundle(bundle)).thenReturn(true);
        assertTrue(creator.isSpringPoweredBundle(bundle, config));
    }

    public void testIsSpringPoweredFalseWithPreProcessor()
    {
        ApplicationContextConfiguration config = mock(ApplicationContextConfiguration.class);
        Bundle bundle = mock(Bundle.class);
        ApplicationContextPreProcessor processor = mock(ApplicationContextPreProcessor.class);

        NonValidatingOsgiApplicationContextCreator creator = new NonValidatingOsgiApplicationContextCreator(Arrays.asList(processor));
        when(config.isSpringPoweredBundle()).thenReturn(false);
        when(processor.isSpringPoweredBundle(bundle)).thenReturn(false);
        assertFalse(creator.isSpringPoweredBundle(bundle, config));
    }
}
