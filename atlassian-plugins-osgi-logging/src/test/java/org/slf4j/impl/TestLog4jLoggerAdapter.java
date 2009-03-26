package org.slf4j.impl;

import junit.framework.TestCase;
import org.apache.log4j.Logger;

public class TestLog4jLoggerAdapter extends TestCase
{
    public void testError()
    {
        Log4jLoggerAdapter log = new Log4jLoggerAdapter(Logger.getLogger(getClass()));
        CountingThrowable t = new CountingThrowable();
        log.error("foo", t);
        assertEquals(1, t.getStringCount());
        assertEquals(1, t.getMessageCount());
    }

    public void testWarn()
    {
        Log4jLoggerAdapter log = new Log4jLoggerAdapter(Logger.getLogger(getClass()));
        CountingThrowable t = new CountingThrowable();
        log.warn("foo", t);
        assertEquals(1, t.getStringCount());
        assertEquals(1, t.getMessageCount());
    }

    public void testTrace()
    {
        Log4jLoggerAdapter log = new Log4jLoggerAdapter(Logger.getLogger(getClass()));
        CountingThrowable t = new CountingThrowable();
        log.trace("foo", t);
        assertEquals(1, t.getStringCount());
        assertEquals(1, t.getMessageCount());
    }

    public void testInfo()
    {
        Log4jLoggerAdapter log = new Log4jLoggerAdapter(Logger.getLogger(getClass()));
        CountingThrowable t = new CountingThrowable();
        log.info("foo", t);
        assertEquals(1, t.getStringCount());
        assertEquals(1, t.getMessageCount());
    }

    private static class CountingThrowable extends RuntimeException
    {
        private int stringCount;
        private int messageCount;

        @Override
        public String toString()
        {
            return "count: "+(stringCount++);
        }

        @Override
        public String getMessage()
        {
            return "msg:" + (messageCount++);
        }

        public int getStringCount()
        {
            return stringCount;
        }

        public int getMessageCount()
        {
            return messageCount;
        }
    }
}
