/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */
package com.atlassian.plugin.servlet.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe lock-less (see note) reference that is not constructed until required.
 * This class is used to maintain a reference to an object that is expensive to
 * create and must be constructed once and once only. Therefore this reference behaves
 * as though the <code>final</code> keyword has been used (you cannot reset it once it
 * has been constructed).
 * <p/>
 * When using this class you need to implement the {@link #create()} method to
 * return the object this reference will hold.
 * <p/>
 * For instance:
 * <pre>
 *  final LazyLoadedReference ref = new LazyLoadedReference()
 *  {
 *    protected Object create() throws Exception
 *    {
 *       // Do some useful object construction here
 *        return new MyObject();
 *    }
 *  };
 * </pre>
 * Then call to get a reference to the object:
 * <pre>
 *   MyObject myLazyLoadedObject = (MyObject) ref.get()
 * </pre>
 * <p/>
 * <strong>Note:</strong> Copied from JIRA com.atlassian.jira.util.concurrent.ThreadsafeLazyLoadedReference and 
 * modified to use generics and java.util.concurrent. 
 * 
 * @since 2.1.0
 */
public abstract class LazyLoadedReference<V>
{
    private final AtomicReference<FutureTask<V>> ref = new AtomicReference<FutureTask<V>>();

    /**
     * Get the lazily loaded reference. If your <code>create()</code> method
     * throws an Exception, calls to <code>get()</code>  will throw a RuntimeException
     * which wraps the previously thrown exception.
     */
    public final V get()
    {
        FutureTask<V> future = ref.get();
        if (future == null)
        {
            // create a Future
            future = new FutureTask<V>(new Callable<V>()
            {
                public V call() throws Exception
                {
                    return create();
                }
            });
            // set the reference only if it is still null
            ref.compareAndSet(null, future);

            // get the future that ref now holds (may be different to above)
            future = ref.get();

            // Remember that the run method is potentially called more than once (by different threads)
            // but the FutireTask implementation ensures that after the first time, all the others are ignored.
            // So the create() method is only invoked once.
            future.run();
        }

        // we guarantee to return a value. So if the InterruptedException is thrown we will
        // set the interrupted flag and try to get the value again - hence the while loop
        while (true)
        {
            try
            {
                return future.get();
            }
            catch (InterruptedException interruptAndTryAgain)
            {
                // if interrupted set our current thread to interrupted but continue trying to get the value
                Thread.currentThread().interrupt();
            }
            catch (ExecutionException e)
            {
                if (e.getCause() != null)
                {
                    throw new InitializationException(e.getCause());
                }
                else
                {
                    throw new InitializationException(e);
                }
            }
        }
    }

    /**
     * The object factory method, guaranteed to be called once and only once.
     * <p>
     * protected abstract V create() throws Exception;
     */
    protected abstract V create() throws Exception;

    /**
     * The factory {@link LazyLoadedReference#create()} method threw an exception.
     */
    public static class InitializationException extends RuntimeException
    {
        InitializationException(Throwable t)
        {
            super(t);
        }
    }
}