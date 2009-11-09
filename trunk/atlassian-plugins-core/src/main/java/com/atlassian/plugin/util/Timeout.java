package com.atlassian.plugin.util;

import java.util.concurrent.TimeUnit;

/**
 * Used to calculate elapsed time for timeouts from when it is created when successively calling
 * blocking methods. Always converts to nanoseconds.
 * <p>
 * Usage:
 * 
 * <pre>
 * Timeout timeout = Timeout.getNanosTimeout(1, TimeUnit.SECONDS);
 * String str = futureString.get(timeout.getTime(), timeout.getUnit());
 * Integer num = futureInt.get(timeout.getTime(), timeout.getUnit());
 * </pre>
 * 
 * where if the first call takes quarter of a second, the second call is passed the equivalent of
 * three-quarters of a second.
 * 
 * @since 2.2
 */
public class Timeout
{
    private static final TimeSupplier NANO_SUPPLIER = new TimeSupplier()
    {
        public long currentTime()
        {
            return System.nanoTime();
        };

        public TimeUnit precision()
        {
            return TimeUnit.NANOSECONDS;
        };
    };

    private static final TimeSupplier MILLIS_SUPPLIER = new TimeSupplier()
    {
        public long currentTime()
        {
            return System.currentTimeMillis();
        };

        public TimeUnit precision()
        {
            return TimeUnit.MILLISECONDS;
        };
    };

    /**
     * Get a {@link Timeout} that uses nanosecond precision. The accuracy will depend on the
     * accuracy of {@link System#nanoTime()}.
     * 
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the <tt>time</tt> argument.
     * @return timeout with {@link TimeUnit#NANOSECONDS} precision.
     */
    public static Timeout getNanosTimeout(final long time, final TimeUnit unit)
    {
        return new Timeout(time, unit, NANO_SUPPLIER);
    }

    /**
     * Get a {@link Timeout} that uses millisecond precision.
     * 
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the <tt>time</tt> argument.
     * @return timeout with {@link TimeUnit#MILLISECONDS} precision.
     */
    public static Timeout getMillisTimeout(final long time, final TimeUnit unit)
    {
        return new Timeout(time, unit, MILLIS_SUPPLIER);
    }

    private final long created;
    private final long time;
    private final TimeSupplier supplier;

    Timeout(final long time, final TimeUnit unit, final TimeSupplier supplier)
    {
        created = supplier.currentTime();
        this.supplier = supplier;
        this.time = this.supplier.precision().convert(time, unit);
    }

    public long getTime()
    {
        return (created + time) - supplier.currentTime();
    }

    public TimeUnit getUnit()
    {
        return supplier.precision();
    }

    public String getRemaining()
    {
        return TimeUnit.SECONDS.convert(getTime(), getUnit()) + " seconds remaining";
    }

    /**
     * Has this timeout expired
     * 
     * @return
     */
    public boolean isExpired()
    {
        return getTime() <= 0;
    }

    /**
     * Supply time and precision to a {@link Timeout}.
     */
    interface TimeSupplier
    {
        long currentTime();

        TimeUnit precision();
    }
}
