package com.atlassian.plugin.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static java.lang.Thread.sleep;

/**
 * Utility methods for synchronising on asynchronous processes
 */
public class WaitUntil
{
    private static final Log log = LogFactory.getLog(WaitUntil.class);

    private WaitUntil() {}

    /**
     * Invokes the wait condition, trying every second for 10 seconds
     *
     * @param waitCondition The condition that determines when to stop waiting
     * @return True if the condition returned true
     */
    public static boolean invoke(WaitCondition waitCondition)
    {
        return invoke(waitCondition, 10);
    }

    /**
     * Invokes the wait condition, trying every second for the configured seconds
     *
     * @param waitCondition The condition that determines when to stop waiting
     * @param tries The number of tries to attempt
     * @return True if the condition returned true
     */
    public static boolean invoke(WaitCondition waitCondition, int tries)
    {
        boolean successful = false;
        for (int count = tries; count > 0; count--)
        {
            if (waitCondition.isFinished())
            {
                successful = true;
                break;
            }

            if (log.isInfoEnabled())
            {
                log.info(waitCondition.getWaitMessage() + ", " + count + " tries remaining");
            }
            try
            {
                sleep(1000);
            } catch (InterruptedException e)
            {
                break;
            }
        }
        return successful;

    }

    /**
     * The condition to determine when to stop waiting
     */
    public static interface WaitCondition
    {
        /**
         * If the condition has been finished
         * @return True if finished and should stop waiting
         */
        boolean isFinished();

        /**
         * Gets the wait message to log for each try
         * @return The string to print describing why the code is waiting
         */
        String getWaitMessage();
    }
}
