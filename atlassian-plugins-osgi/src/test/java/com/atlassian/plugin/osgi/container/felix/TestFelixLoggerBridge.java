package com.atlassian.plugin.osgi.container.felix;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.felix.framework.Logger;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class TestFelixLoggerBridge extends TestCase
{
    private Log log;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        log = mock(Log.class);
    }

    public void testFrameworkLogInfo()
    {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        bridge.doLog(null, Logger.LOG_INFO, "foo", null);
        verify(log).info("foo");
    }

    public void testClassNotFound()
    {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        bridge.doLog(null, Logger.LOG_WARNING, "foo", new ClassNotFoundException("foo"));
        verify(log).debug("Class not found in bundle: foo");
    }

    public void testClassNotFoundOnDebug()
    {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        bridge.doLog(null, Logger.LOG_WARNING, "*** foo", new ClassNotFoundException("*** foo", new ClassNotFoundException("bar")));
        verify(log).debug("Class not found in bundle: *** foo");
    }

    public void testLameClassNotFound()
    {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        verify(log).isDebugEnabled();
        verify(log).isInfoEnabled();
        bridge.doLog(null, Logger.LOG_WARNING, "org.springframework.foo", new ClassNotFoundException("org.springframework.foo"));
        verifyNoMoreInteractions(log);
    }

    public void testLameClassNotFoundInDebug()
    {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        verify(log).isDebugEnabled();
        verify(log).isInfoEnabled();
        bridge.doLog(null, Logger.LOG_WARNING, "*** org.springframework.foo",
                new ClassNotFoundException("*** org.springframework.foo", new ClassNotFoundException("org.springframework.foo")));
        verifyNoMoreInteractions(log);
    }
}
